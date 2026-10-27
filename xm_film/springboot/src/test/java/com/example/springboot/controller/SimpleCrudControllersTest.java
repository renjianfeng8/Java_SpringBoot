package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.Actor;
import com.example.springboot.entity.Admin;
import com.example.springboot.entity.Area;
import com.example.springboot.entity.Notice;
import com.example.springboot.entity.Type;
import com.example.springboot.entity.Video;
import com.example.springboot.service.ActorService;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.AreaService;
import com.example.springboot.service.NoticeService;
import com.example.springboot.service.TypeService;
import com.example.springboot.service.VideoService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 六个「只有 CRUD、没有业务分支」的控制器：Area / Type / Notice / Video / Actor / Admin。
 *
 * <p>它们此前零直接测试。真正会咬人的不是映射本身，而是三处 HTTP 边界：
 * 公开只读前缀（游客能看）、管理员写前缀（USER 不能写）、以及空对象请求体
 * （BaseController 的 requireNonEmptyEntity 必须挡在 service 之前）。
 */
@WebMvcTest({AreaController.class, TypeController.class, NoticeController.class,
        VideoController.class, ActorController.class, AdminController.class})
class SimpleCrudControllersTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private AreaService areaService;
    @MockBean
    private TypeService typeService;
    @MockBean
    private NoticeService noticeService;
    @MockBean
    private VideoService videoService;
    @MockBean
    private ActorService actorService;
    @MockBean
    private AdminService adminService;

    private void loginAs(int userId, RoleEnum role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role.name());
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
    }

    /** 公开只读前缀：匿名可看，且实体字段正确映射到 View */
    @Test
    void publicReadEndpointsMapEntityToViewWithoutToken() throws Exception {
        Area area = new Area();
        area.setId(1);
        area.setTitle("中国大陆");
        when(areaService.selectById(1)).thenReturn(area);

        Type type = new Type();
        type.setId(2);
        type.setTitle("科幻");
        when(typeService.selectById(2)).thenReturn(type);

        Notice notice = new Notice();
        notice.setId(3);
        notice.setTitle("系统公告");
        notice.setContent("内容");
        notice.setTime("2026-10-27 10:00:00");
        when(noticeService.selectById(3)).thenReturn(notice);

        Actor actor = new Actor();
        actor.setId(4);
        actor.setTitle("流浪地球");
        actor.setActorName("吴京");
        when(actorService.selectById(4)).thenReturn(actor);

        mockMvc.perform(get("/api/v1/areas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.title").value("中国大陆"));
        mockMvc.perform(get("/api/v1/types/2"))
                .andExpect(jsonPath("$.data.title").value("科幻"));
        mockMvc.perform(get("/api/v1/notices/3"))
                .andExpect(jsonPath("$.data.content").value("内容"));
        mockMvc.perform(get("/api/v1/actors/4"))
                .andExpect(jsonPath("$.data.actorName").value("吴京"));
    }

    /** 预告片不在公开只读前缀内：匿名 401，USER 登录后可读 */
    @Test
    void videoReadRequiresLogin() throws Exception {
        Video video = new Video();
        video.setId(5);
        video.setTitle("预告片");
        when(videoService.selectById(5)).thenReturn(video);

        mockMvc.perform(get("/api/v1/videos/5"))
                .andExpect(status().isUnauthorized());

        loginAs(11, RoleEnum.USER);
        mockMvc.perform(auth(get("/api/v1/videos/5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("预告片"));
    }

    /** ADMIN_ONLY 前缀：匿名 401，USER 403，ADMIN 才放行 */
    @Test
    void adminOnlyPrefixRejectsNonAdmin() throws Exception {
        loginAs(11, RoleEnum.USER);
        mockMvc.perform(auth(get("/api/v1/admins/1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));

        Admin admin = new Admin();
        admin.setId(1);
        admin.setUsername("root");
        admin.setPassword("secret-hash");
        when(adminService.selectById(1)).thenReturn(admin);

        loginAs(1, RoleEnum.ADMIN);
        mockMvc.perform(auth(get("/api/v1/admins/1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("root"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    /** 五个 ADMIN_WRITE 前缀统一由拦截器挡住 USER；控制器代码本身不用重复判角色 */
    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/areas", "/api/v1/types", "/api/v1/notices",
            "/api/v1/videos", "/api/v1/actors"
    })
    void adminWritePrefixRejectsUser(String path) throws Exception {
        loginAs(11, RoleEnum.USER);

        mockMvc.perform(auth(post(path))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));
    }

    /** 空对象请求体在 BaseController 就被挡下，不能落到 mapper 拼出非法 SQL */
    @Test
    void emptyBodyIsRejectedBeforeService() throws Exception {
        loginAs(1, RoleEnum.ADMIN);

        mockMvc.perform(auth(post("/api/v1/areas"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()))
                .andExpect(jsonPath("$.msg").value("请至少填写一项内容"));

        verify(areaService, never()).add(any());
    }

    /** 新增/删除的 DTO → 实体映射与 service 委派 */
    @Test
    void addAndDeleteDelegateToService() throws Exception {
        loginAs(1, RoleEnum.ADMIN);

        mockMvc.perform(auth(post("/api/v1/types"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"科幻\"}"))
                .andExpect(status().isOk());
        verify(typeService).add(argThat(t -> "科幻".equals(t.getTitle())));

        mockMvc.perform(auth(delete("/api/v1/actors/9")))
                .andExpect(status().isOk());
        verify(actorService).delete(9);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", TOKEN);
    }
}
