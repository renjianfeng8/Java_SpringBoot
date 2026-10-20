package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.PayResult;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.response.SeatOccupancyView;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.OrderedService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 购票主链的控制器层契约：{@link OrderedController} 的<b>状态翻译</b>与<b>越权面</b>。
 *
 * <p><b>为什么在 Controller 层测</b>：这几条不是 service 的业务规则，而是
 * "service 的返回值/入参如何暴露给 HTTP"。此前 22 个 Controller 里 20 个零覆盖，
 * 安全攻坚时每次都得手工用 curl 复现 —— 本类是为那一步铺路。
 *
 * <p><b>鉴权怎么处理</b>：{@code @WebMvcTest} 会加载真正的 {@code AuthInterceptor}
 * （它由 {@code WebMvcConfig} 注册），所以不带 token 的请求一律 401。
 * 本类 mock 掉 {@link JwtUtils#parseToken}，用构造好的 {@link Claims} 模拟不同角色；
 * 拦截器自身的规则已由 {@code AuthInterceptorAccessTest} 覆盖，这里不重复。
 */
@WebMvcTest(OrderedController.class)
class OrderedControllerTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderedService orderedService;

    @MockBean
    private JwtUtils jwtUtils;

    /** 模拟一个已登录用户：拦截器从 Claims 里读 subject 与 role */
    private void loginAs(int userId, RoleEnum role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role.name());
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
    }

    /**
     * 支付超时：service 返回 {@code TIMEOUT_CANCELLED} 时，控制器必须返回 409。
     *
     * <p>业务背景（BL-06 同族）：超时取消是<b>已落库</b>的事实，所以 service 不抛异常
     * 回滚，而是把结果交回控制器翻译。若这里误返回 200，前端会显示"支付成功"，
     * 而订单其实已被取消 —— 资金视角与状态视角直接矛盾。
     */
    @Test
    void payTimeoutIsTranslatedToConflictNotSuccess() throws Exception {
        loginAs(1, RoleEnum.USER);
        when(orderedService.payOrder(eq(7), any(), any(), any()))
                .thenReturn(PayResult.TIMEOUT_CANCELLED);

        mockMvc.perform(auth(put("/api/v1/orders/7/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payPassword\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.BUSINESS_CONFLICT.code()))
                .andExpect(jsonPath("$.msg").value("支付超时，订单已自动取消"));
    }

    /** 支付成功时才是 200 */
    @Test
    void paySuccessReturnsOk() throws Exception {
        loginAs(1, RoleEnum.USER);
        when(orderedService.payOrder(eq(7), any(), any(), any())).thenReturn(PayResult.PAID);

        mockMvc.perform(auth(put("/api/v1/orders/7/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payPassword\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * 选座占用查询：<b>非 USER 必须按匿名视角查</b>（viewerId 传 null）。
     *
     * <p>原因写在控制器注释里：cinema/admin 的 id 与 user 表不同源，直接把 id 传下去
     * 会把"同号用户"的订单误标成 mine，从而在响应里泄露他人的订单号与金额。
     * 这条只能用 mock 断言"到底传了什么"来钉住 —— 属于典型的 Controller 层职责。
     */
    @Test
    void seatOccupancyForNonUserRolePassesNullViewerId() throws Exception {
        // ADMIN 的 id 是 999，与 user 表的 id 空间是两回事
        loginAs(999, RoleEnum.ADMIN);
        when(orderedService.selectSeatOccupancy(eq(42), isNull()))
                .thenReturn(List.of(new SeatOccupancyView()));

        mockMvc.perform(auth(get("/api/v1/orders/seats")).param("recordId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(orderedService).selectSeatOccupancy(eq(42), isNull());
    }

    /** 反之，USER 本人必须带上自己的 id，否则前端无法标出"我的座位" */
    @Test
    void seatOccupancyForUserPassesViewerId() throws Exception {
        loginAs(11, RoleEnum.USER);
        when(orderedService.selectSeatOccupancy(42, 11)).thenReturn(List.of());

        mockMvc.perform(auth(get("/api/v1/orders/seats")).param("recordId", "42"))
                .andExpect(status().isOk());

        verify(orderedService).selectSeatOccupancy(42, 11);
    }

    /**
     * {@code POST /orders} 是 {@code BaseController} 继承来的通用新增，订单不允许走它 ——
     * 必须明确拒绝并引导到 {@code /create}，否则会绕过选座、锁座与库存校验。
     */
    @Test
    void genericAddIsRejectedAndPointsToCreateEndpoint() throws Exception {
        loginAs(1, RoleEnum.USER);

        mockMvc.perform(auth(post("/api/v1/orders"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()))
                .andExpect(jsonPath("$.msg").value("请使用 /api/v1/orders/create 创建订单"));

        verify(orderedService, never()).add(any());
    }

    /** 建单缺少必填字段时是参数错误（400），且不会调用 service */
    @Test
    void createWithMissingFieldsIsParamInvalid() throws Exception {
        loginAs(1, RoleEnum.USER);

        mockMvc.perform(auth(post("/api/v1/orders/create"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()));

        verify(orderedService, never()).createOrder(any(), anyString(), any());
    }

    /**
     * 选座查询缺少 recordId：调用方传错，必须是 400，不能落到兜底变 500。
     * （2026-10-18 的异常路径梳理里，这条曾返回 500。）
     */
    @Test
    void seatOccupancyWithoutRecordIdIsParamInvalid() throws Exception {
        loginAs(1, RoleEnum.USER);

        mockMvc.perform(auth(get("/api/v1/orders/seats")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()));
    }

    /** 业务异常原样透出 code/msg（控制器不吞、不改写） */
    @Test
    void serviceBusinessExceptionKeepsItsCode() throws Exception {
        loginAs(1, RoleEnum.USER);
        when(orderedService.payOrder(eq(7), any(), any(), any()))
                .thenThrow(new CustomException(ErrorCode.UNAUTHORIZED, "支付密码错误"));

        mockMvc.perform(auth(put("/api/v1/orders/7/pay"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payPassword\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.code()))
                .andExpect(jsonPath("$.msg").value("支付密码错误"));
    }

    /** 未登录访问非公开端点：拦截器挡在控制器之前（401，且 service 未被执行） */
    @Test
    void unauthenticatedCancellationIsRejectedBeforeController() throws Exception {
        mockMvc.perform(put("/api/v1/orders/7/cancel"))
                .andExpect(status().isUnauthorized());

        verify(orderedService, never()).cancelOrder(any(), any(), any());
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", TOKEN);
    }
}
