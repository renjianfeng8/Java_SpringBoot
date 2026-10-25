package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流：同一 role + username 连续失败 5 次后锁 15 分钟。
 *
 * <p>为什么放在这里：登录的三个入口（User/Admin/Cinema）都经 {@link AuthService} 分发，
 * 在这一层计数不会漏掉某个角色。支付密码已有独立的 5 次/15 分钟计数，两者互不影响。
 *
 * <p>shortcut: 单进程内存计数，重启即清空；多实例部署时各节点不共享，需换 Redis/DB。
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    /** 防止大量随机用户名把内存撑爆；超过后先清掉已解锁的条目。 */
    private static final int MAX_ENTRIES = 10_000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    /** 测试用：可注入固定时钟。 */
    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String role, String username) {
        Attempt attempt = attempts.get(key(role, username));
        if (attempt != null && attempt.lockedUntil != null && attempt.lockedUntil.isAfter(clock.instant())) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS, "登录失败次数过多，请15分钟后再试");
        }
    }

    public void onFailure(String role, String username) {
        Attempt attempt = attempts.computeIfAbsent(key(role, username), ignored -> new Attempt());
        synchronized (attempt) {
            Instant now = clock.instant();
            if (attempt.lockedUntil != null && !attempt.lockedUntil.isAfter(now)) {
                attempt.failures = 0;
                attempt.lockedUntil = null;
            }
            attempt.failures++;
            if (attempt.failures >= MAX_FAILURES) {
                attempt.lockedUntil = now.plus(LOCK_DURATION);
            }
        }
        cleanupIfNeeded();
    }

    public void onSuccess(String role, String username) {
        attempts.remove(key(role, username));
    }

    private void cleanupIfNeeded() {
        if (attempts.size() <= MAX_ENTRIES) {
            return;
        }
        Instant now = clock.instant();
        attempts.entrySet().removeIf(entry -> {
            Instant lockedUntil = entry.getValue().lockedUntil;
            return lockedUntil == null || lockedUntil.isBefore(now);
        });
        if (attempts.size() > MAX_ENTRIES) {
            attempts.clear();
        }
    }

    private static String key(String role, String username) {
        return (role == null ? "" : role) + ":" + (username == null ? "" : username);
    }

    private static final class Attempt {
        private int failures;
        private volatile Instant lockedUntil;
    }
}
