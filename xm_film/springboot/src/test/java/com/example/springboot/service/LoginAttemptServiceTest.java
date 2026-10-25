package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService(
            Clock.fixed(Instant.parse("2026-10-25T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void locksAfterFiveFailuresAndResetsOnSuccess() {
        for (int i = 0; i < 5; i++) {
            service.onFailure("USER", "u1");
        }

        assertThatThrownBy(() -> service.checkAllowed("USER", "u1"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCode())
                        .isEqualTo(ErrorCode.TOO_MANY_REQUESTS.code()));

        service.onSuccess("USER", "u1");
        assertThatCode(() -> service.checkAllowed("USER", "u1")).doesNotThrowAnyException();
    }

    @Test
    void differentRolesAndAccountsDoNotShareCounter() {
        for (int i = 0; i < 5; i++) {
            service.onFailure("USER", "u1");
        }

        assertThatCode(() -> service.checkAllowed("USER", "u2")).doesNotThrowAnyException();
        assertThatCode(() -> service.checkAllowed("ADMIN", "u1")).doesNotThrowAnyException();
    }

    /** 库里的 username 比较忽略大小写与尾部空格，计数也必须按同一口径归一，否则换个写法就能重来 5 次 */
    @Test
    void caseAndTrailingSpaceVariantsShareCounter() {
        for (int i = 0; i < 5; i++) {
            service.onFailure("USER", " U1 ");
        }

        assertThatThrownBy(() -> service.checkAllowed("USER", "u1"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCode())
                        .isEqualTo(ErrorCode.TOO_MANY_REQUESTS.code()));
    }
}
