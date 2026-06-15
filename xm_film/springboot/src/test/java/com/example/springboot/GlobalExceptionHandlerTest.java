package com.example.springboot;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.controller.AuthController;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.CinemaService;
import com.example.springboot.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
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

    // ========== 参数校验测试 ==========

    @Test
    void loginRejectsBlankUsernameWithParamInvalidCode() throws Exception {
        String body = """
                {"username":"","password":"123456","role":"USER"}
                """;

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
                {"username":"zhangsan","password":"","role":"USER"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("密码")));
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
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("角色")));
    }

    // ========== 业务逻辑测试 ==========

    @Test
    void loginWithValidUserCredentialsReturnsToken() throws Exception {
        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("zhangsan");
        mockUser.setRole("USER");
        when(userService.login(any(Account.class))).thenReturn(mockUser);
        when(jwtUtils.generateToken(1, "USER")).thenReturn("mock-token");

        String body = """
                {"username":"zhangsan","password":"user123","role":"USER"}
                """;

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
                {"username":"wrong","password":"wrong","role":"USER"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("角色")));
    }

    @Test
    void loginWhenServiceThrowsCustomExceptionReturnsErrorCode() throws Exception {
        when(userService.login(any(Account.class)))
                .thenThrow(new CustomException("403", "账号已禁用"));

        String body = """
                {"username":"blocked","password":"123456","role":"USER"}
                """;

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
                {"username":"zhangsan","password":"user123","role":"USER"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.msg").isNotEmpty());
    }
}
