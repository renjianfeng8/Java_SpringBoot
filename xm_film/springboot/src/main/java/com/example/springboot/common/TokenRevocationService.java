package com.example.springboot.common;

import com.example.springboot.mapper.AdminMapper;
import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * 按账号吊销已签发的 JWT：改密（本人或管理员重置）后，旧令牌立即失效。
 *
 * <p>做法是「签发时间截断」而不是逐 token 黑名单：吊销时记下``此刻''，解析时只要
 * token 的 {@code iat} 早于该时刻就拒绝。这样不需要给每枚令牌记 jti、也不需要
 * 知道它什么时候过期，每个账号最多一条记录，不会越用越大。
 *
 * <p>吊销点存在 admin/user/cinema 的 {@code token_revoked_before} 列里，而不是内存：
 * 重启或换实例不会把旧令牌放回来，代价是每次解析令牌多一次主键查询。
 *
 * <p>只查 3 张账号表、不做通用映射：角色枚举只有这三种，{@code role} 取自令牌的
 * role claim，拼不出表名注入。
 */
@Service
public class TokenRevocationService {

    private final AdminMapper adminMapper;

    private final UserMapper userMapper;

    private final CinemaMapper cinemaMapper;

    private final Clock clock;

    public TokenRevocationService(AdminMapper adminMapper, UserMapper userMapper, CinemaMapper cinemaMapper,
                                  Clock clock) {
        this.adminMapper = adminMapper;
        this.userMapper = userMapper;
        this.cinemaMapper = cinemaMapper;
        this.clock = clock;
    }

    /** 让该账号当前已签发的全部令牌失效；之后的登录会拿到 iat 不早于该秒的新令牌 */
    public void revoke(String role, Integer accountId) {
        if (role == null || accountId == null) {
            return;
        }
        long now = clock.instant().getEpochSecond();
        switch (role) {
            case "ADMIN" -> adminMapper.updateRevokedBefore(accountId, now);
            case "USER" -> userMapper.updateRevokedBefore(accountId, now);
            case "CINEMA" -> cinemaMapper.updateRevokedBefore(accountId, now);
            default -> {
                return;
            }
        }
    }

    /**
     * @param issuedAt token 的 iat（NumericDate 秒）；缺失或账号已不存在时按未吊销处理
     */
    public boolean isRevoked(String role, String subject, Number issuedAt) {
        if (role == null || subject == null || issuedAt == null) {
            return false;
        }
        Integer id;
        try {
            id = Integer.valueOf(subject);
        } catch (NumberFormatException e) {
            return false;
        }
        Long cutoff = switch (role) {
            case "ADMIN" -> adminMapper.selectRevokedBefore(id);
            case "USER" -> userMapper.selectRevokedBefore(id);
            case "CINEMA" -> cinemaMapper.selectRevokedBefore(id);
            default -> null;
        };
        // 严格小于：同一秒内改密后立即登录拿到的新令牌不应被误杀
        return cutoff != null && issuedAt.longValue() < cutoff;
    }
}
