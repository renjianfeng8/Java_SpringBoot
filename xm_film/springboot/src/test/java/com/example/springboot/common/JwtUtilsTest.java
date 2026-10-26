package com.example.springboot.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** iat/exp 以 NumericDate 秒值写入，解析与过期判定必须与 jjwt 原生 Date 行为一致 */
class JwtUtilsTest {

    private static final String KEY = "01234567890123456789012345678901";

    @Test
    void generatedTokenCarriesUserRoleAndOneDayExpiry() {
        JwtUtils jwt = new JwtUtils(KEY, 86_400_000L);

        Claims claims = jwt.parseToken(jwt.generateToken(7, "USER"));

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims).containsEntry("role", "USER");
        assertThat(claims.getExpiration().toInstant().getEpochSecond()
                - claims.getIssuedAt().toInstant().getEpochSecond()).isEqualTo(86_400L);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtils jwt = new JwtUtils(KEY, -60_000L);
        String token = jwt.generateToken(7, "USER");

        assertThatThrownBy(() -> jwt.parseToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shortSigningKeyIsRejectedAtStartup() {
        assertThatThrownBy(() -> new JwtUtils("too-short", 1_000L)).isInstanceOf(IllegalStateException.class);
    }
}
