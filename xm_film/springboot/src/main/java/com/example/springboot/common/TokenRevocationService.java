package com.example.springboot.common;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按账号吊销已签发的 JWT：改密（本人或管理员重置）后，旧令牌立即失效。
 *
 * <p>做法是「签发时间截断」而不是逐 token 黑名单：吊销时记下``此刻''，解析时只要
 * token 的 {@code iat} 早于该时刻就拒绝。这样不需要给每枚令牌记 jti、也不需要
 * 知道它什么时候过期，每个账号最多一条记录，内存随账号数有界，不会越用越大。
 *
 * <p>为什么需要：JWT 无状态，改密本身不会让已泄露的旧令牌失效；管理员重置密码后
 * 攻击者仍能用旧令牌操作到过期。截断点让「改密」真正等于「踢下线」。
 *
 * <p>shortcut: 单进程内存记录，重启即清空（重启后旧令牌重新可用，但 JWT 本身
 * 仍有 exp 上限）、多实例不共享；需要严格吊销时换 DB 列 {@code token_version}。
 */
@Service
public class TokenRevocationService {

    /** role:id → 吊销生效的 epoch 秒 */
    private final Map<String, Long> revokedBefore = new ConcurrentHashMap<>();

    private final Clock clock;

    public TokenRevocationService(Clock clock) {
        this.clock = clock;
    }

    /** 让该账号当前已签发的全部令牌失效；之后的登录会拿到 iat 不早于该秒的新令牌 */
    public void revoke(String role, Integer accountId) {
        if (role == null || accountId == null) {
            return;
        }
        revokedBefore.put(key(role, String.valueOf(accountId)), clock.instant().getEpochSecond());
    }

    /**
     * @param issuedAt token 的 iat（NumericDate 秒）；缺失时按不早于吊销点处理，避免误杀
     */
    public boolean isRevoked(String role, String subject, Number issuedAt) {
        if (role == null || subject == null || issuedAt == null) {
            return false;
        }
        Long cutoff = revokedBefore.get(key(role, subject));
        // 严格小于：同一秒内改密后立即登录拿到的新令牌不应被误杀
        return cutoff != null && issuedAt.longValue() < cutoff;
    }

    private static String key(String role, String subject) {
        return role + ":" + subject;
    }
}
