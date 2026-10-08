package com.example.springboot.service;

import com.example.springboot.dto.response.PayPasswordState;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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

    @InjectMocks
    private PayPasswordService payPasswordService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private PayPasswordState stateWith(String rawPayPassword) {
        PayPasswordState state = new PayPasswordState();
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
    void verifyOldPassword_withCorrectPassword_passesWithoutWriting() {
        when(userMapper.selectPayPasswordState(USER_ID)).thenReturn(stateWith("123456"));

        payPasswordService.verifyOldPassword(USER_ID, "123456");

        verify(userMapper, never()).updatePayPassword(anyInt(), anyString());
    }

    @Test
    void verifyOldPassword_withWrongPassword_throwsAndCountsTheFailure() {
        when(userMapper.selectPayPasswordState(USER_ID)).thenReturn(stateWith("123456"));
        when(userMapper.selectPayPasswordErrorCount(USER_ID)).thenReturn(1);

        assertThatThrownBy(() -> payPasswordService.verifyOldPassword(USER_ID, "000000"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("还可重试 4 次");

        verify(userMapper).incrementPayPasswordFailure(eq(USER_ID), eq(5), anyString());
        verify(userMapper, never()).updatePayPassword(anyInt(), anyString());
    }

    @Test
    void verifyOldPassword_whenLocked_throwsLockMessageAndDoesNotCountAgain() {
        PayPasswordState state = stateWith("123456");
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
    void verifyOldPassword_beforeAnyPasswordWasSet_throws() {
        PayPasswordState state = new PayPasswordState();
        state.setId(USER_ID);
        when(userMapper.selectPayPasswordState(USER_ID)).thenReturn(state);

        assertThatThrownBy(() -> payPasswordService.verifyOldPassword(USER_ID, "123456"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("尚未设置支付密码");
    }

    @Test
    void verifyLoginPassword_withCorrectPassword_passesWithoutWriting() {
        when(userMapper.selectById(USER_ID)).thenReturn(userWith(encoder.encode("user123")));

        payPasswordService.verifyLoginPassword(USER_ID, "user123");

        verify(userMapper, never()).updatePayPassword(anyInt(), anyString());
    }

    /** 存量种子账号的登录密码是明文（规则 6），判定口径必须与 login / reset 一致 */
    @Test
    void verifyLoginPassword_withPlaintextStoredPassword_passes() {
        when(userMapper.selectById(USER_ID)).thenReturn(userWith("123"));

        payPasswordService.verifyLoginPassword(USER_ID, "123");

        verify(userMapper, never()).updatePayPassword(anyInt(), anyString());
    }

    @Test
    void verifyLoginPassword_withWrongPassword_throwsWithoutWriting() {
        when(userMapper.selectById(USER_ID)).thenReturn(userWith(encoder.encode("user123")));

        assertThatThrownBy(() -> payPasswordService.verifyLoginPassword(USER_ID, "nope"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("登录密码错误");

        verify(userMapper, never()).updatePayPassword(anyInt(), anyString());
    }

    /** 设置 / 重设仍然是一条原子路径：登录密码对了才写，且写完就是新码 */
    @Test
    void resetWithLoginPassword_withCorrectPassword_stillWritesTheNewPassword() {
        when(userMapper.selectById(USER_ID)).thenReturn(userWith(encoder.encode("user123")));

        assertThatCode(() -> payPasswordService.resetWithLoginPassword(USER_ID, "user123", "654321"))
                .doesNotThrowAnyException();

        verify(userMapper).updatePayPassword(eq(USER_ID), anyString());
    }
}
