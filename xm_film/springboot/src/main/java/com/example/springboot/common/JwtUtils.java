package com.example.springboot.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class JwtUtils {

    // 读 jwt.signing-key 而非 jwt.secret：本机用户级环境变量 JWT_SECRET 会经宽松绑定覆盖
    // jwt.secret（环境变量优先级高于 application.yml），改名前这行在本机取到的是别的项目的密钥。
    private final String secret;

    private final long expire;

    private final TokenRevocationService revocation;

    public JwtUtils(@Value("${jwt.signing-key}") String secret, @Value("${jwt.expire}") long expire,
                    TokenRevocationService revocation) {
        // 密钥必须来自 .env（JWT_SIGNING_KEY）。空值或过短直接拒绝启动，
        // 避免把仓库里的字符串当成可用密钥，也避免 HS256 在弱密钥下被离线爆破。
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SIGNING_KEY 未配置或短于 32 字节；请复制 xm_film/springboot/.env.example 为 .env 并填写");
        }
        this.secret = secret;
        this.expire = expire;
        this.revocation = revocation;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Integer userId, String role) {
        // iat/exp 直接写 NumericDate 秒值，不经过 java.util.Date（jjwt 0.11 的 Date API）；线上格式一致
        long issuedAt = Instant.now().getEpochSecond();
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("role", role)
                .claim("iat", issuedAt)
                .claim("exp", issuedAt + expire / 1000)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        // 改密后旧令牌立即失效：调用方（AuthInterceptor）统一把异常映射成 401
        Number issuedAt = claims.get("iat", Number.class);
        if (revocation.isRevoked(claims.get("role", String.class), claims.getSubject(), issuedAt)) {
            throw new io.jsonwebtoken.JwtException("令牌已失效，请重新登录");
        }
        return claims;
    }
}
