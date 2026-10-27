package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.service.PayPasswordService;
import com.example.springboot.service.WalletService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 账户端点：余额与支付密码只属于 USER，且归属只认 token。
 *
 * <p>此前零直接测试。这里钉住两件容易被“重构掉”的事：
 * 非 USER 不能读余额；所有写入口都不接受请求体里的 userId。
 */
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private WalletService walletService;
    @MockBean
    private PayPasswordService payPasswordService;

    private void loginAs(int userId, RoleEnum role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role.name());
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
    }

    @Test
    void summaryReturnsOwnBalanceForUser() throws Exception {
        loginAs(11, RoleEnum.USER);
        when(walletService.getBalance(11)).thenReturn(new BigDecimal("128.50"));
        when(payPasswordService.hasPayPassword(11)).thenReturn(true);

        mockMvc.perform(auth(get("/api/v1/account/summary")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(128.50))
                .andExpect(jsonPath("$.data.hasPayPassword").value(true));
    }

    /** admin / cinema 与 user 是三套主体，没有账户余额 */
    @Test
    void summaryRejectsNonUserRole() throws Exception {
        loginAs(7, RoleEnum.CINEMA);

        mockMvc.perform(auth(get("/api/v1/account/summary")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()))
                .andExpect(jsonPath("$.msg").value("仅普通用户有账户余额"));

        verify(walletService, never()).getBalance(any());
    }

    @Test
    void changePayPasswordUsesTokenUserId() throws Exception {
        loginAs(11, RoleEnum.USER);

        mockMvc.perform(auth(put("/api/v1/account/pay-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"111111\",\"payPassword\":\"222222\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(payPasswordService).changePayPassword(11, "111111", "222222");
    }

    /** 首次设置/忘记重设：验登录密码，但仍只作用于 token 里的用户 */
    @Test
    void resetPayPasswordUsesTokenUserId() throws Exception {
        loginAs(11, RoleEnum.USER);

        mockMvc.perform(auth(put("/api/v1/account/pay-password/reset"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginPassword\":\"login-pwd\",\"payPassword\":\"333333\"}"))
                .andExpect(status().isOk());

        verify(payPasswordService).resetWithLoginPassword(11, "login-pwd", "333333");
    }

    /** 6 位数字约束由 @Valid 在控制器入口挡住，不能落到 service 才报错 */
    @Test
    void invalidPayPasswordIsRejectedByValidation() throws Exception {
        loginAs(11, RoleEnum.USER);

        mockMvc.perform(auth(put("/api/v1/account/pay-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"111111\",\"payPassword\":\"12ab\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()));

        verify(payPasswordService, never()).changePayPassword(any(), any(), any());
    }

    @Test
    void verifyOldPasswordDelegates() throws Exception {
        loginAs(11, RoleEnum.USER);

        mockMvc.perform(auth(post("/api/v1/account/pay-password/verify-old"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"111111\"}"))
                .andExpect(status().isOk());

        verify(payPasswordService).verifyOldPassword(11, "111111");
    }

    @Test
    void anonymousAccountAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/account/summary"))
                .andExpect(status().isUnauthorized());

        verify(payPasswordService, never()).verifyOldPassword(any(), eq(""));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", TOKEN);
    }
}
