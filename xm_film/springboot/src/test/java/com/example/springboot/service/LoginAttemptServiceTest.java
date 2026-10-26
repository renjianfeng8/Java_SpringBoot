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
            service.onFailure("USER", null, "u1");
        }

        assertThatThrownBy(() -> service.checkAllowed("USER", null, "u1"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCode())
                        .isEqualTo(ErrorCode.TOO_MANY_REQUESTS.code()));

        service.onSuccess("USER", null, "u1");
        assertThatCode(() -> service.checkAllowed("USER", null, "u1")).doesNotThrowAnyException();
    }

    @Test
    void differentRolesAndAccountsDoNotShareCounter() {
        for (int i = 0; i < 5; i++) {
            service.onFailure("USER", null, "u1");
        }

        assertThatCode(() -> service.checkAllowed("USER", null, "u2")).doesNotThrowAnyException();
        assertThatCode(() -> service.checkAllowed("ADMIN", null, "u1")).doesNotThrowAnyException();
    }

    /** 已解析到账号时按 id 计数：同一账号的重音/大小写拼写共享同一把锁 */
    @Test
    void spellingVariantsShareTheAccountCounter() {
        for (int i = 0; i < 5; i++) {
            service.onFailure("USER", 7, "usér1");
        }

        assertThatThrownBy(() -> service.checkAllowed("USER", 7, "u1"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCode())
                        .isEqualTo(ErrorCode.TOO_MANY_REQUESTS.code()));
    }

    /** 未解析到账号时退回请求用户名：同一文本的大小写/尾随空格变体不能各占一条计数 */
    @Test
    void caseAndTrailingSpaceVariantsShareFallbackCounter() {
        for (int i = 0; i < 5; i++) {
            service.onFailure("USER", null, " U1 ");
        }

        assertThatThrownBy(() -> service.checkAllowed("USER", null, "u1"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCode())
                        .isEqualTo(ErrorCode.TOO_MANY_REQUESTS.code()));
    }
}
