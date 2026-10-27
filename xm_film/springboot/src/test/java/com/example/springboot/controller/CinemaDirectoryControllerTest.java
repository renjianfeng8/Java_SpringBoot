package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.response.CinemaImportView;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.service.CinemaDirectoryImportService;
import com.example.springboot.service.CinemaDirectoryService;
import com.github.pagehelper.PageInfo;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 影院名录：整条前缀（读 + 写）都只对 ADMIN 开放。
 *
 * <p>此前零直接测试。除了前缀门禁，另一条契约是导入必须显式选城市 ——
 * 空列表不能变成“抓全国”这种默认行为。
 */
@WebMvcTest(CinemaDirectoryController.class)
class CinemaDirectoryControllerTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private CinemaDirectoryService directoryService;
    @MockBean
    private CinemaDirectoryImportService importService;

    private void loginAs(int userId, RoleEnum role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role.name());
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
    }

    /** 管理员之外，连 GET /page 都不行（名录没有前台消费方） */
    @Test
    void nonAdminIsRejectedEvenForReads() throws Exception {
        loginAs(7, RoleEnum.CINEMA);

        mockMvc.perform(auth(get("/api/v1/cinema-directory/page")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));

        verify(directoryService, never()).selectPage(any(), anyInt(), anyInt());
    }

    @Test
    void adminPageMapsDirectoryToView() throws Exception {
        loginAs(1, RoleEnum.ADMIN);
        CinemaDirectory directory = new CinemaDirectory();
        directory.setId(1);
        directory.setName("万达影城");
        directory.setCity("北京");
        when(directoryService.selectPage(any(), eq(1), eq(12)))
                .thenReturn(PageInfo.of(List.of(directory)));

        mockMvc.perform(auth(get("/api/v1/cinema-directory/page")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].name").value("万达影城"))
                .andExpect(jsonPath("$.data.list[0].city").value("北京"));
    }

    @Test
    void filtersReturnCitiesAndBrands() throws Exception {
        loginAs(1, RoleEnum.ADMIN);
        when(directoryService.distinctCities()).thenReturn(List.of("北京"));
        when(directoryService.distinctBrands()).thenReturn(List.of("万达"));

        mockMvc.perform(auth(get("/api/v1/cinema-directory/filters")))
                .andExpect(jsonPath("$.data.cities[0]").value("北京"))
                .andExpect(jsonPath("$.data.brands[0]").value("万达"));
    }

    @Test
    void importWithoutCitiesIsRejected() throws Exception {
        loginAs(1, RoleEnum.ADMIN);

        mockMvc.perform(auth(post("/api/v1/cinema-directory/import/preview"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()))
                .andExpect(jsonPath("$.msg").value("请至少选择一个城市"));

        verify(importService, never()).preview(any());
    }

    @Test
    void previewAndImportDelegateSelectedCities() throws Exception {
        loginAs(1, RoleEnum.ADMIN);
        when(importService.preview(List.of("北京")))
                .thenReturn(new CinemaImportView(10, 8, 2, List.of(), List.of()));
        when(importService.importAll(List.of("北京"))).thenReturn(8);

        mockMvc.perform(auth(post("/api/v1/cinema-directory/import/preview"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cities\":[\"北京\"]}"))
                .andExpect(jsonPath("$.data.total").value(10))
                .andExpect(jsonPath("$.data.freshCount").value(8));

        mockMvc.perform(auth(post("/api/v1/cinema-directory/import"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cities\":[\"北京\"]}"))
                .andExpect(jsonPath("$.data").value(8));

        verify(importService).preview(List.of("北京"));
        verify(importService).importAll(List.of("北京"));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", TOKEN);
    }
}
