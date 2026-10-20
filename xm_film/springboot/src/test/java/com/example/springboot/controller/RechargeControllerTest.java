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
 * <p><b>本类刻意不测试、也不固化的一点</b>：{@code POST /recharges/{id}/callback} 目前
 * 仅凭请求体里的 {@code success:true} 就把钱记入钱包 —— 没有任何服务端可验证的凭证。
 * 这是已登记的 **SEC-01**（见 {@code audit/backend-backlog.md} §安全），修复方向是
 * 引入支付平台签名/查询接口校验，届时本类的回调相关断言需要跟着改。
 * 在此之前，本类只钉住"归属"与"幂等"这两条<b>已经成立且不应被破坏</b>的规则 ——
 * 不把"任意用户能给自己充值"写进断言，否则修复安全时会被测试挡住。
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
     * 回调的<b>归属规则</b>已成立：拿别人的单据 id 去回调，service 抛 403，
     * 控制器必须原样透出（不能吞掉变成 200）。
     *
     * <p>注意：这<b>不等于</b>"回调是安全的" —— 自己给自己回调目前依然能成功（SEC-01）。
     * 本用例守的是"越权访问他人单据"这一半。
     */
    @Test
    void callbackOnSomeoneElsesOrderSurfacesForbidden() throws Exception {
        loginAs(11, RoleEnum.USER);
        doThrow(new CustomException(ErrorCode.FORBIDDEN, "无权操作该充值单据"))
                .when(rechargeService).handleCallback(eq(99), anyBoolean(), any(), any(), any());

        mockMvc.perform(auth(post("/api/v1/recharges/99/callback"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"success\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()))
                .andExpect(jsonPath("$.msg").value("无权操作该充值单据"));
    }

    /** 重复回调必须被拒（幂等），且错误码是 409 而非 500 */
    @Test
    void duplicateCallbackIsConflict() throws Exception {
        loginAs(11, RoleEnum.USER);
        doThrow(new CustomException(ErrorCode.BUSINESS_CONFLICT, "该充值单据已处理，不可重复回调"))
                .when(rechargeService).handleCallback(eq(7), anyBoolean(), any(), any(), any());

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
        loginAs(11, RoleEnum.USER);

        mockMvc.perform(auth(post("/api/v1/recharges/7/callback"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"success\":true,\"remark\":\"ok\",\"userId\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(rechargeService).handleCallback(7, true, "ok",
                RoleEnum.USER.name(), 11);
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
