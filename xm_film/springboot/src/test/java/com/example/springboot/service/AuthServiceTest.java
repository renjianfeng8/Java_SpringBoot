package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AdminService adminService;

    @Mock
    private CinemaService cinemaService;

    @Mock
    private UserService userService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    @Test
    void failedLoginCountsAndRethrows() {
        Account account = account("USER", "u1");
        when(userService.login(account))
                .thenThrow(new CustomException(ErrorCode.UNAUTHORIZED, "账号或密码错误"));

        assertThatThrownBy(() -> authService.login(account))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("账号或密码错误");

        verify(loginAttemptService).onFailure("USER", "u1");
        verify(loginAttemptService, never()).onSuccess(anyString(), anyString());
    }

    @Test
    void successfulLoginClearsCounter() {
        Account account = account("USER", "u1");
        User user = new User();
        user.setId(1);
        when(userService.login(account)).thenReturn(user);

        assertThat(authService.login(account)).isSameAs(user);

        verify(loginAttemptService).onSuccess("USER", "u1");
        verify(loginAttemptService, never()).onFailure(anyString(), anyString());
    }

    private static Account account(String role, String username) {
        Account account = new Account();
        account.setRole(role);
        account.setUsername(username);
        return account;
    }
}
