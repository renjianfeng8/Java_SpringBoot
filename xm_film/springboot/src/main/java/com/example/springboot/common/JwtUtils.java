package com.example.springboot.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    // 读 jwt.signing-key 而非 jwt.secret：本机用户级环境变量 JWT_SECRET 会经宽松绑定覆盖
    // jwt.secret（环境变量优先级高于 application.yml），改名前这行在本机取到的是别的项目的密钥。
    private final String secret;

    private final long expire;

    public JwtUtils(@Value("${jwt.signing-key}") String secret, @Value("${jwt.expire}") long expire) {
        // 密钥必须来自 .env（JWT_SIGNING_KEY）。空值或过短直接拒绝启动，
        // 避免把仓库里的字符串当成可用密钥，也避免 HS256 在弱密钥下被离线爆破。
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SIGNING_KEY 未配置或短于 32 字节；请复制 xm_film/springboot/.env.example 为 .env 并填写");
        }
        this.secret = secret;
        this.expire = expire;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Integer userId, String role) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expire))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
