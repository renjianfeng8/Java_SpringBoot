package com.example.springboot.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** iat/exp 以 NumericDate 秒值写入，解析与过期判定必须与 jjwt 原生 Date 行为一致 */
class JwtUtilsTest {

    private static final String KEY = "01234567890123456789012345678901";

    /** 吊销点固定在 2000 年：本测试里签出的令牌都晚于它，视为未吊销 */
    private static final Clock PAST = Clock.fixed(Instant.parse("2000-01-01T00:00:00Z"), ZoneOffset.UTC);

    /** 吊销点固定在 2100 年：本测试里签出的令牌都早于它，视为已吊销 */
    private static final Clock FUTURE = Clock.fixed(Instant.parse("2100-01-01T00:00:00Z"), ZoneOffset.UTC);

    private static JwtUtils jwt(String key, long expire, Clock clock) {
        return new JwtUtils(key, expire, new TokenRevocationService(clock));
    }

    @Test
    void generatedTokenCarriesUserRoleAndOneDayExpiry() {
        JwtUtils jwt = jwt(KEY, 86_400_000L, PAST);

        Claims claims = jwt.parseToken(jwt.generateToken(7, "USER"));

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims).containsEntry("role", "USER");
        assertThat(claims.getExpiration().toInstant().getEpochSecond()
                - claims.getIssuedAt().toInstant().getEpochSecond()).isEqualTo(86_400L);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtils jwt = jwt(KEY, -60_000L, PAST);
        String token = jwt.generateToken(7, "USER");

        assertThatThrownBy(() -> jwt.parseToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shortSigningKeyIsRejectedAtStartup() {
        assertThatThrownBy(() -> jwt("too-short", 1_000L, PAST)).isInstanceOf(IllegalStateException.class);
    }

    /** 改密后旧令牌必须先于新令牌失效，且只影响该账号 */
    @Test
    void revokedAccountTokensAreRejectedButOtherAccountsAreNot() {
        TokenRevocationService revocation = new TokenRevocationService(FUTURE);
        JwtUtils jwt = new JwtUtils(KEY, 86_400_000L, revocation);
        String userToken = jwt.generateToken(7, "USER");
        String adminToken = jwt.generateToken(1, "ADMIN");

        revocation.revoke("USER", 7);

        assertThatThrownBy(() -> jwt.parseToken(userToken)).isInstanceOf(JwtException.class);
        assertThat(jwt.parseToken(adminToken).getSubject()).isEqualTo("1");
    }

    /** 吊销点早于令牌签发时间时不影响它（同一秒内改密后重新登录的令牌不能被误杀） */
    @Test
    void tokenIssuedAfterRevocationPointStaysValid() {
        TokenRevocationService revocation = new TokenRevocationService(PAST);
        JwtUtils jwt = new JwtUtils(KEY, 86_400_000L, revocation);
        String token = jwt.generateToken(7, "USER");

        revocation.revoke("USER", 7);

        assertThat(jwt.parseToken(token).getSubject()).isEqualTo("7");
    }
}
