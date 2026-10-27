package com.example.springboot;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.controller.AuthController;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.AuthService;
import com.example.springboot.service.CinemaService;
import com.example.springboot.service.LoginAttemptService;
import com.example.springboot.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, AuthService.class})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AdminService adminService;

    @MockBean
    UserService userService;

    @MockBean
    CinemaService cinemaService;

    @MockBean
    JwtUtils jwtUtils;

    @MockBean
    LoginAttemptService loginAttemptService;

    @MockBean
    com.example.springboot.common.TokenRevocationService tokenRevocationService;

    /** 让真实拦截器放行，同时把登录态交给控制器；中间件本身由 AuthInterceptorAccessTest 覆盖 */
    private void authenticateAs(String role) {
        Claims claims = mock(Claims.class);
        when(claims.get("role", String.class)).thenReturn(role);
        when(claims.getSubject()).thenReturn("1");
        when(jwtUtils.parseToken("test-token")).thenReturn(claims);
    }

    @Test
    void passwordResetRejectsNonAdmin() throws Exception {
        authenticateAs(RoleEnum.USER.name());
        String body = """
                {"id":7,"role":"USER","newPassword":"123456"}
                """;

        mockMvc.perform(put("/api/v1/auth/password/reset")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"));
    }

    @Test
    void passwordResetRoutesTargetRoleForAdmin() throws Exception {
        authenticateAs(RoleEnum.ADMIN.name());
        String body = """
                {"id":7,"role":"USER","newPassword":"123456"}
                """;

        mockMvc.perform(put("/api/v1/auth/password/reset")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
        verify(userService).resetPassword(7, "123456");
    }

    // ========== 参数校验测试 ==========
    @Test
    void loginRejectsBlankUsernameWithParamInvalidCode() throws Exception {
        String body = """
                {"username":"","password":"123456","role":"%s"}
                """.formatted(RoleEnum.USER.name());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.msg").isNotEmpty());
    }

    @Test
    void loginRejectsBlankPassword() throws Exception {
        String body = """
                {"username":"zhangsan","password":"","role":"%s"}
                """.formatted(RoleEnum.USER.name());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.msg").value(containsString("密码")));
    }

    @Test
    void loginRejectsBlankRole() throws Exception {
        String body = """
                {"username":"zhangsan","password":"123456","role":""}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.msg").value(containsString("角色")));
    }

    // ========== 业务逻辑测试 ==========

    @Test
    void loginWithValidUserCredentialsReturnsToken() throws Exception {
        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("zhangsan");
        mockUser.setRole(RoleEnum.USER.name());
        when(userService.login(any(Account.class))).thenReturn(mockUser);
        when(jwtUtils.generateToken(1, RoleEnum.USER.name())).thenReturn("mock-token");

        String body = """
                {"username":"zhangsan","password":"user123","role":"%s"}
                """.formatted(RoleEnum.USER.name());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.token").value("mock-token"));
    }

    @Test
    void loginWithInvalidCredentialsReturnsError() throws Exception {
        when(userService.login(any(Account.class))).thenReturn(null);

        String body = """
                {"username":"wrong","password":"wrong","role":"%s"}
                """.formatted(RoleEnum.USER.name());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.msg").value(containsString("角色")));
    }

    @Test
    void loginWhenServiceThrowsCustomExceptionReturnsErrorCode() throws Exception {
        when(userService.login(any(Account.class)))
                .thenThrow(new CustomException("403", "账号已禁用"));

        String body = """
                {"username":"blocked","password":"123456","role":"%s"}
                """.formatted(RoleEnum.USER.name());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.msg").value("账号已禁用"));
    }

    @Test
    void loginWhenServiceThrowsRuntimeReturnsSystemError() throws Exception {
        when(userService.login(any(Account.class)))
                .thenThrow(new RuntimeException("数据库连接失败"));

        String body = """
                {"username":"zhangsan","password":"user123","role":"%s"}
                """.formatted(RoleEnum.USER.name());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.msg").isNotEmpty());
    }
}
