package com.example.springboot.common.config;

import com.example.springboot.common.PasswordMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 一次性把存量明文口令转成 BCrypt。
 *
 * <p>为什么需要：{@link PasswordMatcher} 不再接受“库中值 == 提交值”，而演示库和手工插入的
 * 旧账号可能仍存明文。启动时迁移一次，之后每次启动只跑一条空查询。
 * 只迁移登录口令与支付口令；不打印任何口令。
 */
@Component
public class PlaintextPasswordMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PlaintextPasswordMigration.class);

    /** 四类明文行一次取回；target 由 SQL 字面量决定，不来自请求。 */
    private static final String SELECT_PLAINTEXT = """
            SELECT 'user' AS target, id, password AS pwd FROM user
              WHERE password IS NOT NULL AND password <> '' AND password NOT LIKE '$2%'
            UNION ALL
            SELECT 'admin', id, password FROM admin
              WHERE password IS NOT NULL AND password <> '' AND password NOT LIKE '$2%'
            UNION ALL
            SELECT 'cinema', id, password FROM cinema
              WHERE password IS NOT NULL AND password <> '' AND password NOT LIKE '$2%'
            UNION ALL
            SELECT 'user_pay', id, pay_password FROM user
              WHERE pay_password IS NOT NULL AND pay_password <> '' AND pay_password NOT LIKE '$2%'
            """;

    private final JdbcTemplate jdbcTemplate;

    public PlaintextPasswordMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(SELECT_PLAINTEXT);
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update(updateSql((String) row.get("target")),
                    PasswordMatcher.encode((String) row.get("pwd")), row.get("id"));
        }
        if (!rows.isEmpty()) {
            log.info("已将 {} 条存量明文口令迁移为 BCrypt", rows.size());
        }
    }

    private static String updateSql(String target) {
        return switch (target) {
            case "user" -> "UPDATE user SET password = ? WHERE id = ?";
            case "admin" -> "UPDATE admin SET password = ? WHERE id = ?";
            case "cinema" -> "UPDATE cinema SET password = ? WHERE id = ?";
            case "user_pay" -> "UPDATE user SET pay_password = ? WHERE id = ?";
            default -> throw new IllegalStateException("未知的迁移目标: " + target);
        };
    }
}
