package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.RechargeOrder;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.RechargeService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 充值单据的控制器层契约：<b>登录主体与回调入参如何传到 service</b>。
 *
 * <p>回调入口现在只接受 ADMIN（SEC-01 的修复）：用户只能提交申请，不能自己确认到账。
 * 本类钉住三条规则：主体取自 token、USER 调用回调会得到 403、重复回调得到 409。
 */
@WebMvcTest(RechargeController.class)
class RechargeControllerTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RechargeService rechargeService;

    @MockBean
    private JwtUtils jwtUtils;

    private void loginAs(int userId, RoleEnum role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role.name());
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
    }

    /**
     * 建单时，登录主体必须<b>来自 token</b>，不能让请求体指定 userId。
     * {@code RechargeCreateRequest} 里根本没有 userId 字段，这条钉住"控制器没有把它补回去"。
     */
    @Test
    void createTakesUserIdFromTokenNotFromBody() throws Exception {
        loginAs(11, RoleEnum.USER);
        when(rechargeService.createRecharge(eq(11), eq(RoleEnum.USER.name()), any()))
                .thenReturn(new RechargeOrder());

        mockMvc.perform(auth(post("/api/v1/recharges"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"userId\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(rechargeService).createRecharge(eq(11), eq(RoleEnum.USER.name()), any());
    }

    /** 金额缺失是参数错误（400），不能落到 service，更不能变 500 */
    @Test
    void createWithoutAmountIsParamInvalid() throws Exception {
        loginAs(11, RoleEnum.USER);

        mockMvc.perform(auth(post("/api/v1/recharges"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()));

        verify(rechargeService, never()).createRecharge(any(), anyString(), any());
    }

    /**
     * 回调只接受 ADMIN。普通用户即使操作自己的单据也必须得到 403 ——
     * 这是 SEC-01 的核心断言，不能只靠前端隐藏按钮。
     */
    @Test
    void userCallbackSurfacesForbidden() throws Exception {
        loginAs(11, RoleEnum.USER);
        doThrow(new CustomException(ErrorCode.FORBIDDEN, "仅管理员可审核充值单据"))
                .when(rechargeService).handleCallback(eq(99), anyBoolean(), any(), eq(RoleEnum.USER.name()));

        mockMvc.perform(auth(post("/api/v1/recharges/99/callback"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"success\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()))
                .andExpect(jsonPath("$.msg").value("仅管理员可审核充值单据"));
    }

    /** 重复回调必须被拒（幂等），且错误码是 409 而非 500 */
    @Test
    void duplicateCallbackIsConflict() throws Exception {
        loginAs(1, RoleEnum.ADMIN);
        doThrow(new CustomException(ErrorCode.BUSINESS_CONFLICT, "该充值单据已处理，不可重复回调"))
                .when(rechargeService).handleCallback(eq(7), anyBoolean(), any(), eq(RoleEnum.ADMIN.name()));

        mockMvc.perform(auth(post("/api/v1/recharges/7/callback"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"success\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.BUSINESS_CONFLICT.code()))
                .andExpect(jsonPath("$.msg").value("该充值单据已处理，不可重复回调"));
    }

    /** 回调主体同样取自 token，不从请求体读（请求体里只有 success / remark） */
    @Test
    void callbackPassesTokenIdentityToService() throws Exception {
        loginAs(1, RoleEnum.ADMIN);

        mockMvc.perform(auth(post("/api/v1/recharges/7/callback"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"success\":true,\"remark\":\"ok\",\"userId\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(rechargeService).handleCallback(7, true, "ok", RoleEnum.ADMIN.name());
    }

    /** 未登录不能建单（不是公开端点） */
    @Test
    void unauthenticatedCreateIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/recharges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100}"))
                .andExpect(status().isUnauthorized());

        verify(rechargeService, never()).createRecharge(any(), anyString(), any());
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", TOKEN);
    }
}
