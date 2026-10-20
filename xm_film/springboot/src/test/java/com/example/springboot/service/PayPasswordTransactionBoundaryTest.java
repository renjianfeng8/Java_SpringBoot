package com.example.springboot.service;

import com.example.springboot.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 守卫（Sonar {@code java:S2229}）：支付密码的失败计数必须独立提交，
 * <b>不能被调用方的事务回滚带走</b>。
 *
 * <p><b>缺陷形态</b>：{@code changePayPassword} / {@code verifyOldPassword} 通过
 * {@code this} 调用标注了 {@code @Transactional(REQUIRES_NEW)} 的 {@code checkOldPassword}。
 * Spring 代理不拦截同类自调用，{@code REQUIRES_NEW} 被静默忽略；只要调用方在事务里，
 * 「输错 5 次锁定 15 分钟」的计数就会随回滚消失 —— 限次形同虚设。
 *
 * <p><b>判据</b>：开一个外层事务 → 在事务内调 {@code verifyOldPassword} 输错一次 →
 * 外层回滚 → <b>换一条连接</b>读库。修复前计数是 0，修复后必须是 1。
 * 这条断言直接对应「限次会不会失效」，而不是实现细节。
 *
 * <p><b>为什么本类不能加 {@code @Transactional}</b>：测试自身若被包在事务里，
 * 就观察不到「外层回滚、内层提交」的真实结果，用例会永远绿。自建数据在 finally 删除。
 */
@SpringBootTest
@ActiveProfiles("ci")
class PayPasswordTransactionBoundaryTest {

    @Autowired
    private PayPasswordService payPasswordService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void failureCountSurvivesRollbackOfCallerTransaction() {
        String username = "tx_pwd_" + System.nanoTime();
        String hash = new BCryptPasswordEncoder().encode("123456");
        try {
            jdbcTemplate.update(
                    "INSERT INTO user (username, password, role, pay_password, pay_pwd_error_count) "
                            + "VALUES (?, ?, 'USER', ?, 0)",
                    username, hash, hash);
            Integer userId = jdbcTemplate.queryForObject(
                    "SELECT id FROM user WHERE username = ?", Integer.class, username);

            assertThatThrownBy(() -> transactionTemplate.execute(status -> {
                payPasswordService.verifyOldPassword(userId, "000000");
                return null;
            })).isInstanceOf(CustomException.class);

            Integer failures = jdbcTemplate.queryForObject(
                    "SELECT pay_pwd_error_count FROM user WHERE id = ?", Integer.class, userId);
            assertThat(failures)
                    .as("失败计数必须在 REQUIRES_NEW 事务里提交，不得随调用方回滚消失")
                    .isEqualTo(1);
        } finally {
            jdbcTemplate.update("DELETE FROM user WHERE username = ?", username);
        }
    }
}
