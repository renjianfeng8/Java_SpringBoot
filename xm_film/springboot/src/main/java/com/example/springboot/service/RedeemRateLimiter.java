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
 * 匿名核销的失败限流：同一来源 IP 在 10 分钟内失败 30 次后拒绝继续尝试。
 *
 * <p>为什么需要：{@code POST /tickets/redeem} 是全仓唯一的匿名写入口，入参只有 6 位取票码
 * （31^8 空间）。码本身不可猜，但没有节流时脚本可以持续试码；限流把「离线爆破」降为
 * 「十分钟 30 次」，配合一次性状态迁移，实际可利用性趋近于零。
 *
 * <p>只在失败时计数，成功即清零：正常取票机的重复扫码（已核销后再扫）也会计入失败，
 * 30 次/10 分钟对一台公共设备足够宽松。
 *
 * <p>shortcut: 单进程内存计数，重启即清空、多实例不共享；也按 {@code remoteAddr} 计数，
 * 反向代理后所有人共用一个来源。部署形态变化时换 Redis/网关限流。
 */
@Service
public class RedeemRateLimiter {

    private static final int MAX_FAILURES = 30;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    /** 防止大量来源 IP 撑爆内存；超过后清掉已过窗的条目。 */
    private static final int MAX_ENTRIES = 10_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RedeemRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String clientKey) {
        Window window = windows.get(clientKey);
        if (window != null && window.blocked(clock.instant())) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS, "尝试次数过多，请稍后再试");
        }
    }

    public void onFailure(String clientKey) {
        windows.computeIfAbsent(clientKey, ignored -> new Window()).recordFailure(clock.instant());
        cleanupIfNeeded();
    }

    public void onSuccess(String clientKey) {
        windows.remove(clientKey);
    }

    private void cleanupIfNeeded() {
        if (windows.size() <= MAX_ENTRIES) {
            return;
        }
        Instant now = clock.instant();
        windows.entrySet().removeIf(entry -> entry.getValue().expired(now));
        if (windows.size() > MAX_ENTRIES) {
            windows.clear();
        }
    }

    private static final class Window {
        private Instant startedAt;
        private int failures;

        synchronized void recordFailure(Instant now) {
            if (startedAt == null || expired(now)) {
                startedAt = now;
                failures = 0;
            }
            failures++;
        }

        synchronized boolean blocked(Instant now) {
            return !expired(now) && failures >= MAX_FAILURES;
        }

        synchronized boolean expired(Instant now) {
            return startedAt == null || !now.isBefore(startedAt.plus(WINDOW));
        }
    }
}
