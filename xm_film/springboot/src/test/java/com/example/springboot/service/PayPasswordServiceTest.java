package com.example.springboot.service;

import com.example.springboot.dto.response.PayPasswordStateView;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 「验证身份」两步化后新增的两个只验不写的方法。
 *
 * 这两个方法的要害不是"验得对"，而是"验完什么都不写"以及"原支付密码这一支照旧限次" ——
 * 前者保证第一步永远不会成为一条绕过写入校验的旁路，后者保证它不会成为一条不限次的试错通道。
 */
@ExtendWith(MockitoExtension.class)
class PayPasswordServiceTest {

    private static final Integer USER_ID = 6;

    @Mock
    private UserMapper userMapper;

    private RecordingTransactionManager transactionManager;

    private PayPasswordService payPasswordService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        transactionManager = new RecordingTransactionManager();
        payPasswordService = new PayPasswordService(userMapper, new TransactionTemplate(transactionManager));
    }

    private PayPasswordStateView stateWith(String rawPayPassword) {
        PayPasswordStateView state = new PayPasswordStateView();
        state.setId(USER_ID);
        state.setPayPassword(encoder.encode(rawPayPassword));
        state.setErrorCount(0);
        return state;
    }

    private User userWith(String storedLoginPassword) {
        User user = new User();
        user.setId(USER_ID);
        user.setPassword(storedLoginPassword);
        return user;
    }

    @Test
    void verifyOldPasswordWithCorrectPasswordPassesWithoutWriting() {
        when(userMapper.selectPayPasswordState(USER_ID)).thenReturn(stateWith("123456"));

        payPasswordService.verifyOldPassword(USER_ID, "123456");

        verify(userMapper, never()).updatePayPassword(anyInt(), anyString());
    }

    @Test
    void verifyOldPasswordWithWrongPasswordThrowsAndCountsTheFailure() {
        when(userMapper.selectPayPasswordState(USER_ID)).thenReturn(stateWith("123456"));
        when(userMapper.selectPayPasswordErrorCount(USER_ID)).thenReturn(1);

        assertThatThrownBy(() -> payPasswordService.verifyOldPassword(USER_ID, "000000"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("还可重试 4 次");

        verify(userMapper).incrementPayPasswordFailure(eq(USER_ID), eq(5), anyString());
        verify(userMapper, never()).updatePayPassword(anyInt(), anyString());
    }

    @Test
    void verifyOldPasswordWhenLockedThrowsLockMessageAndDoesNotCountAgain() {
        PayPasswordStateView state = stateWith("123456");
        state.setLockedUntil(LocalDateTime.now(ZoneId.systemDefault())
                .plusMinutes(10)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        when(userMapper.selectPayPasswordState(USER_ID)).thenReturn(state);

        assertThatThrownBy(() -> payPasswordService.verifyOldPassword(USER_ID, "000000"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("已锁定");

        verify(userMapper, never()).incrementPayPasswordFailure(anyInt(), anyInt(), anyString());
    }

    @Test
    void verifyOldPasswordBeforeAnyPasswordWasSetThrows() {
        PayPasswordStateView state = new PayPasswordStateView();
        state.setId(USER_ID);
        when(userMapper.selectPayPasswordState(USER_ID)).thenReturn(state);

        assertThatThrownBy(() -> payPasswordService.verifyOldPassword(USER_ID, "123456"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("尚未设置支付密码");
    }

    @Test
    void verifyLoginPasswordWithCorrectPasswordPassesWithoutWriting() {
        when(userMapper.selectById(USER_ID)).thenReturn(userWith(encoder.encode("user123")));

        payPasswordService.verifyLoginPassword(USER_ID, "user123");

        verify(userMapper, never()).updatePayPassword(anyInt(), anyString());
    }

    /** SEC-03 修复后，库里的明文不再能登录；存量明文由启动迁移转成 BCrypt。 */
    @Test
    void verifyLoginPasswordWithPlaintextStoredPasswordThrows() {
        when(userMapper.selectById(USER_ID)).thenReturn(userWith("123"));

        assertThatThrownBy(() -> payPasswordService.verifyLoginPassword(USER_ID, "123"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("登录密码错误");

        verify(userMapper, never()).updatePayPassword(anyInt(), anyString());
    }

    @Test
    void verifyLoginPasswordWithWrongPasswordThrowsWithoutWriting() {
        when(userMapper.selectById(USER_ID)).thenReturn(userWith(encoder.encode("user123")));

        assertThatThrownBy(() -> payPasswordService.verifyLoginPassword(USER_ID, "nope"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("登录密码错误");

        verify(userMapper, never()).updatePayPassword(anyInt(), anyString());
    }

    /** 设置 / 重设仍然是一条原子路径：登录密码对了才写，且写完就是新码 */
    @Test
    void resetWithLoginPasswordWithCorrectPasswordStillWritesTheNewPassword() {
        when(userMapper.selectById(USER_ID)).thenReturn(userWith(encoder.encode("user123")));

        assertThatCode(() -> payPasswordService.resetWithLoginPassword(USER_ID, "user123", "654321"))
                .doesNotThrowAnyException();

        verify(userMapper).updatePayPassword(eq(USER_ID), anyString());
    }

    /**
     * Sonar {@code java:S2229} 的守卫：改密/只验不写这两条入口必须真的新开事务。
     *
     * <p>旧实现是 {@code @Transactional(REQUIRES_NEW)} + 同类自调用，代理被绕过，
     * 失败计数会跟着调用方的事务一起回滚（限次形同虚设）。这里断言实际传入
     * 事务管理器的传播级别是 REQUIRES_NEW，防止有人把实现改回自调用。
     */
    @Test
    void verifyOldPasswordRunsInRequiresNewTransaction() {
        when(userMapper.selectPayPasswordState(USER_ID)).thenReturn(stateWith("123456"));
        when(userMapper.selectPayPasswordErrorCount(USER_ID)).thenReturn(1);

        assertThatThrownBy(() -> payPasswordService.verifyOldPassword(USER_ID, "000000"))
                .isInstanceOf(CustomException.class);

        assertThat(transactionManager.startedWith).isNotNull();
        assertThat(transactionManager.startedWith.getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 记录「开事务时拿到的定义」，并让回调在不碰真实数据库的情况下执行 */
    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        private TransactionDefinition startedWith;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            this.startedWith = definition;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // 测试用假事务：没有真实资源，提交是空操作
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // 测试用假事务：没有真实资源，回滚是空操作
        }
    }
}
