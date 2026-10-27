package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.TmdbClient;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.response.TmdbBackfillView;
import com.example.springboot.dto.response.TmdbImportView;
import com.example.springboot.service.TmdbBackfillService;
import com.example.springboot.service.TmdbService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TMDB 导入入口：整条前缀仅 ADMIN。
 *
 * <p>这不是“后台工具就默认安全”的假设题：/movie/{id} 会建类型/地区/演职人员行并落盘图片，
 * /backfill-videos 会改写影片数据。两个 GET 也必须拦，不能只拦写方法。
 */
@WebMvcTest(TmdbController.class)
class TmdbControllerTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private TmdbService tmdbService;
    @MockBean
    private TmdbBackfillService tmdbBackfillService;

    private void loginAs(int userId, RoleEnum role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role.name());
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
    }

    /** 非管理员连 search / movie 这两个 GET 都不能碰 */
    @Test
    void nonAdminIsRejectedForReadEndpoints() throws Exception {
        loginAs(11, RoleEnum.USER);

        mockMvc.perform(auth(get("/api/v1/tmdb/search").param("query", "流浪地球")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));

        mockMvc.perform(auth(get("/api/v1/tmdb/movie/550")))
                .andExpect(status().isForbidden());

        verify(tmdbService, never()).search(anyString());
        verify(tmdbService, never()).preview(550);
    }

    @Test
    void blankQueryIsParamInvalid() throws Exception {
        loginAs(1, RoleEnum.ADMIN);

        mockMvc.perform(auth(get("/api/v1/tmdb/search").param("query", "  ")))
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()))
                .andExpect(jsonPath("$.msg").value("请输入要搜索的影片名"));

        verify(tmdbService, never()).search(anyString());
    }

    @Test
    void adminSearchDelegates() throws Exception {
        loginAs(1, RoleEnum.ADMIN);
        when(tmdbService.search("流浪地球"))
                .thenReturn(List.of(new TmdbClient.SearchHit(550, "流浪地球", "The Wandering Earth",
                        "2019", "/poster.jpg", "简介")));

        mockMvc.perform(auth(get("/api/v1/tmdb/search").param("query", "流浪地球")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tmdbId").value(550))
                .andExpect(jsonPath("$.data[0].title").value("流浪地球"));
    }

    @Test
    void adminPreviewAndBackfillDelegate() throws Exception {
        loginAs(1, RoleEnum.ADMIN);
        TmdbImportView preview = new TmdbImportView();
        preview.setTitle("流浪地球");
        preview.setTime(125);
        when(tmdbService.preview(550)).thenReturn(preview);
        when(tmdbBackfillService.backfillVideos())
                .thenReturn(new TmdbBackfillView(10, 3, List.of()));

        mockMvc.perform(auth(get("/api/v1/tmdb/movie/550")))
                .andExpect(jsonPath("$.data.title").value("流浪地球"))
                .andExpect(jsonPath("$.data.time").value(125));

        mockMvc.perform(auth(post("/api/v1/tmdb/backfill-videos")))
                .andExpect(jsonPath("$.data.scanned").value(10))
                .andExpect(jsonPath("$.data.updated").value(3));

        verify(tmdbService).preview(550);
        verify(tmdbBackfillService).backfillVideos();
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", TOKEN);
    }
}
