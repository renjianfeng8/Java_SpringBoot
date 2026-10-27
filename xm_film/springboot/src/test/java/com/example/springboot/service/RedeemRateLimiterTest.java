package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 匿名核销限流：挡住脚本持续试码，同时不能误伤正常取票机的重复扫码。
 * 用可推进的时钟验证「窗口到期自动放行」，不依赖 sleep。
 */
class RedeemRateLimiterTest {

    private static final Instant START = Instant.parse("2026-10-27T00:00:00Z");

    /** 可手动前进的时钟 */
    private static final class MutableClock extends Clock {
        private Instant now = START;

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void thirtyFailuresBlockTheSameClient() {
        MutableClock clock = new MutableClock();
        RedeemRateLimiter limiter = new RedeemRateLimiter(clock);

        for (int i = 0; i < 30; i++) {
            limiter.onFailure("10.0.0.1");
        }

        assertThatThrownBy(() -> limiter.checkAllowed("10.0.0.1"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> org.assertj.core.api.Assertions
                        .assertThat(((CustomException) e).getCode())
                        .isEqualTo(ErrorCode.TOO_MANY_REQUESTS.code()));
    }

    @Test
    void otherClientsAreUnaffected() {
        MutableClock clock = new MutableClock();
        RedeemRateLimiter limiter = new RedeemRateLimiter(clock);

        for (int i = 0; i < 30; i++) {
            limiter.onFailure("10.0.0.1");
        }

        assertThatCode(() -> limiter.checkAllowed("10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void successClearsTheWindow() {
        MutableClock clock = new MutableClock();
        RedeemRateLimiter limiter = new RedeemRateLimiter(clock);

        for (int i = 0; i < 30; i++) {
            limiter.onFailure("10.0.0.1");
        }
        limiter.onSuccess("10.0.0.1");

        assertThatCode(() -> limiter.checkAllowed("10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void windowExpiryLetsTheClientTryAgain() {
        MutableClock clock = new MutableClock();
        RedeemRateLimiter limiter = new RedeemRateLimiter(clock);

        for (int i = 0; i < 30; i++) {
            limiter.onFailure("10.0.0.1");
        }
        clock.now = START.plus(Duration.ofMinutes(10));

        assertThatCode(() -> limiter.checkAllowed("10.0.0.1")).doesNotThrowAnyException();

        // 新窗口从第一次失败重新计数：29 次仍可试，第 30 次才封
        for (int i = 0; i < 29; i++) {
            limiter.onFailure("10.0.0.1");
        }
        assertThatCode(() -> limiter.checkAllowed("10.0.0.1")).doesNotThrowAnyException();
    }
}
