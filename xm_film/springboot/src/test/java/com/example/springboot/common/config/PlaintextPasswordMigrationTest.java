package com.example.springboot.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 启动迁移必须真的把明文换成 BCrypt：SEC-03 修复后登录只认 BCrypt，
 * 存量账号若没迁移就会被锁在门外。
 */
@SpringBootTest
@ActiveProfiles("ci")
class PlaintextPasswordMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlaintextPasswordMigration migration;

    @Test
    void convertsPlaintextLoginAndPayPasswords() {
        String username = "migration-test-" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO user (username, password, role, pay_password) VALUES (?, 'plain-login', 'USER', 'plain-pay')",
                username);
        try {
            migration.run();

            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT password, pay_password FROM user WHERE username = ?", username);
            assertThat(row.get("password").toString()).startsWith("$2");
            assertThat(row.get("pay_password").toString()).startsWith("$2");
        } finally {
            jdbcTemplate.update("DELETE FROM user WHERE username = ?", username);
        }
    }
}
