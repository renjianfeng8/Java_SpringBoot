package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.service.StatisticsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 统计大盘：整条端点只对 ADMIN 开放。
 *
 * <p>该前缀没有进 AuthInterceptor 的任何前缀表，因此权限判断只在控制器里，
 * 没有第二道网 —— 这条测试就是那道网。
 */
@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private StatisticsService statisticsService;

    private void loginAs(int userId, RoleEnum role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role.name());
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
    }

    @Test
    void adminCanReadOverview() throws Exception {
        loginAs(1, RoleEnum.ADMIN);
        when(statisticsService.overview()).thenReturn(Map.of("todayRevenue", 100, "pendingPickupOrders", 3L));

        mockMvc.perform(get("/api/v1/statistics/overview").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.todayRevenue").value(100))
                .andExpect(jsonPath("$.data.pendingPickupOrders").value(3));
    }

    @Test
    void nonAdminIsForbiddenAndServiceNotCalled() throws Exception {
        loginAs(7, RoleEnum.CINEMA);

        mockMvc.perform(get("/api/v1/statistics/overview").header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));

        verify(statisticsService, never()).overview();
    }

    @Test
    void anonymousIsRejectedByInterceptor() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/overview"))
                .andExpect(status().isUnauthorized());

        verify(statisticsService, never()).overview();
    }
}
