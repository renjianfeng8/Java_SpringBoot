package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.response.TodayRevenueView;
import com.example.springboot.entity.Film;
import com.example.springboot.service.FilmService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 影片控制器：只读端点公开，写端点（含删除）必须走管理员门禁；
 * 删除走 deleteGuarded 而不是逐条 delete，避免绕过“引用检查 + 锁序”。
 */
@WebMvcTest(FilmController.class)
class FilmControllerTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private FilmService filmService;
    @MockBean
    private OrderedService orderedService;

    private void loginAs(int userId, RoleEnum role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role.name());
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
    }

    @Test
    void searchWithoutTitleIsParamInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/films/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()));

        verify(filmService, never()).selectByTitle(any());
    }

    @Test
    void searchMapsFilmToView() throws Exception {
        Film film = new Film();
        film.setId(1);
        film.setTitle("流浪地球");
        film.setScore(9.1);
        when(filmService.selectByTitle("流浪")).thenReturn(List.of(film));

        mockMvc.perform(get("/api/v1/films/search").param("title", "流浪"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("流浪地球"))
                .andExpect(jsonPath("$.data[0].score").value(9.1));
    }

    @Test
    void byCinemaWithoutCinemaIdIsParamInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/films/by-cinema"))
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()));
    }

    /** 删除必须一次交给 deleteGuarded（锁父行 + 数引用 + 删），不能退回逐条 delete */
    @Test
    void deleteUsesGuardedPath() throws Exception {
        loginAs(1, RoleEnum.ADMIN);

        mockMvc.perform(auth(delete("/api/v1/films/3")))
                .andExpect(status().isOk());

        verify(filmService).deleteGuarded(List.of(3));
        verify(filmService, never()).delete(3);
    }

    @Test
    void batchDeleteUsesGuardedPath() throws Exception {
        loginAs(1, RoleEnum.ADMIN);

        mockMvc.perform(auth(delete("/api/v1/films/batch"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[3,4]"))
                .andExpect(status().isOk());

        verify(filmService).deleteGuarded(List.of(3, 4));
    }

    /** 管理员写前缀由拦截器兜底，控制器层的空对象守卫仍要挡住“全空实体” */
    @Test
    void emptyFilmBodyIsRejectedBeforeService() throws Exception {
        loginAs(1, RoleEnum.ADMIN);

        mockMvc.perform(auth(post("/api/v1/films"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()))
                .andExpect(jsonPath("$.msg").value("请至少填写一项内容"));

        verify(filmService, never()).add(any());
    }

    /** 今日票房是匿名公开数据 */
    @Test
    void todayRevenueIsPublic() throws Exception {
        when(orderedService.todayPaidRevenue())
                .thenReturn(new TodayRevenueView(new BigDecimal("1234.50"), "2026-10-27 12:00:00"));

        mockMvc.perform(get("/api/v1/films/box-office/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1234.50))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-10-27 12:00:00"));
    }

    /** 榜单查询复用同一个 View 映射（typeList → TypeView） */
    @Test
    void markTopMapsTypeList() throws Exception {
        Film film = new Film();
        film.setId(2);
        film.setTitle("测试");
        com.example.springboot.entity.Type type = new com.example.springboot.entity.Type();
        type.setId(5);
        type.setTitle("科幻");
        film.setTypeList(List.of(type));
        when(filmService.getMarkTop(any())).thenReturn(List.of(film));

        mockMvc.perform(get("/api/v1/films/mark/top"))
                .andExpect(jsonPath("$.data[0].typeList[0].title").value("科幻"));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", TOKEN);
    }
}
