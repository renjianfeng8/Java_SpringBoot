package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.FundFlow;
import com.example.springboot.service.FundFlowService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 资金流水：只读账本，非公开端点。
 *
 * <p>此前零直接测试。关键契约是 <b>查询范围只能由 token 决定</b>：
 * 普通用户看自己的流水，影院看本影院，管理员看全部；请求体里的 userId 不能越权。
 */
@WebMvcTest(FundFlowController.class)
class FundFlowControllerTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private FundFlowService fundFlowService;

    private void loginAs(int userId, RoleEnum role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role.name());
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
    }

    @Test
    void pageUsesTokenScopeAndMapsUserName() throws Exception {
        loginAs(11, RoleEnum.USER);
        FundFlow flow = new FundFlow();
        flow.setId(1);
        flow.setUserId(11);
        flow.setUserName("张三");
        flow.setSource("购票");
        flow.setChangeAmount(new BigDecimal("-45.50"));
        flow.setBalanceAfter(new BigDecimal("100.00"));
        when(fundFlowService.selectScoped(any(), eq("USER"), eq(11))).thenReturn(List.of(flow));

        mockMvc.perform(get("/api/v1/fund-flows/page")
                        .header("Authorization", TOKEN)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.list[0].userName").value("张三"))
                .andExpect(jsonPath("$.data.list[0].changeAmount").value(-45.50));

        verify(fundFlowService).selectScoped(any(), eq("USER"), eq(11));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/fund-flows/page"))
                .andExpect(status().isUnauthorized());
    }
}
