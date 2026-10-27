package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Admin;
import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private com.example.springboot.common.TokenRevocationService tokenRevocationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void failedLoginCountsAndRethrows() {
        Account account = account("USER", "u1");
        when(userService.findIdByUsername("u1")).thenReturn(7);
        when(userService.login(account))
                .thenThrow(new CustomException(ErrorCode.UNAUTHORIZED, "账号或密码错误"));

        assertThatThrownBy(() -> authService.login(account))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("账号或密码错误");

        verify(loginAttemptService).onFailure("USER", 7, "u1");
        verify(loginAttemptService, never()).onSuccess(anyString(), any(), anyString());
    }

    @Test
    void successfulLoginClearsCounter() {
        Account account = account("USER", "u1");
        User user = new User();
        user.setId(1);
        when(userService.findIdByUsername("u1")).thenReturn(1);
        when(userService.login(account)).thenReturn(user);

        assertThat(authService.login(account)).isSameAs(user);

        verify(loginAttemptService).onSuccess("USER", 1, "u1");
        verify(loginAttemptService, never()).onFailure(anyString(), any(), anyString());
    }

    @Test
    void adminLoginCountsAgainstAdminAccountId() {
        Account account = account("ADMIN", "root");
        Admin admin = new Admin();
        admin.setId(3);
        when(adminService.findIdByUsername("root")).thenReturn(3);
        when(adminService.login(account)).thenReturn(admin);

        assertThat(authService.login(account)).isSameAs(admin);

        verify(loginAttemptService).onSuccess("ADMIN", 3, "root");
    }

    @Test
    void cinemaLoginCountsAgainstCinemaAccountId() {
        Account account = account("CINEMA", "cinema");
        Cinema cinema = new Cinema();
        cinema.setId(5);
        when(cinemaService.findIdByUsername("cinema")).thenReturn(5);
        when(cinemaService.login(account)).thenReturn(cinema);

        assertThat(authService.login(account)).isSameAs(cinema);

        verify(loginAttemptService).onSuccess("CINEMA", 5, "cinema");
    }

    /** 库的 collation 把重音拼写解析到同一账号，计数就必须落到同一 id，而不是原样文本 */
    @Test
    void spellingVariantCountsAgainstTheResolvedAccount() {
        Account account = account("USER", "usér1");
        when(userService.findIdByUsername("usér1")).thenReturn(7);
        when(userService.login(account))
                .thenThrow(new CustomException(ErrorCode.UNAUTHORIZED, "账号或密码错误"));

        assertThatThrownBy(() -> authService.login(account))
                .isInstanceOf(CustomException.class);

        verify(loginAttemptService).onFailure("USER", 7, "usér1");
    }

    /** 改密成功即吊销该账号全部旧令牌；角色非法时不能误发吊销 */
    @Test
    void passwordChangeRevokesExistingTokens() {
        Account account = account("USER", "u1");
        account.setId(7);
        account.setNewPassword("new-pass");

        authService.updatePassword(account);

        verify(userService).updatePassword(account);
        verify(tokenRevocationService).revoke("USER", 7);
    }

    @Test
    void adminPasswordResetRevokesTargetAccountTokens() {
        authService.resetPassword("CINEMA", 5, "new-pass", 1);

        verify(cinemaService).resetPassword(5, "new-pass");
        verify(tokenRevocationService).revoke("CINEMA", 5);
    }

    private static Account account(String role, String username) {
        Account account = new Account();
        account.setRole(role);
        account.setUsername(username);
        return account;
    }
}
