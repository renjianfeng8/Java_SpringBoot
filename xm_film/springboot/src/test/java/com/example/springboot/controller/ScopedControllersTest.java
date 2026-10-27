package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.Mark;
import com.example.springboot.entity.Record;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.MarkService;
import com.example.springboot.service.RecordService;
import com.example.springboot.service.RoomService;
import com.example.springboot.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 四个「带角色范围」的控制器：User / Record / Room / Mark。
 *
 * <p>这些端点的共同风险不是 CRUD 映射，而是<b>身份从 token 来、不是从请求体来</b>：
 * 影院角色只能看/改本影院、普通用户只能动自己的评价、管理员才能建用户。
 * 这里用 MockMvc 走真实 AuthInterceptor，断言控制器到底把什么 role/userId 传给了 service。
 */
@WebMvcTest({UserController.class, RecordController.class, RoomController.class, MarkController.class})
class ScopedControllersTest {

    private static final String TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private UserService userService;
    @MockBean
    private RecordService recordService;
    @MockBean
    private RoomService roomService;
    @MockBean
    private MarkService markService;

    private void loginAs(int userId, RoleEnum role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role.name());
        when(jwtUtils.parseToken(anyString())).thenReturn(claims);
    }

    /** 影院账号看排片：范围由 token 的 userId 写入查询条件，不信任前端传的 cinemaId */
    @Test
    void recordListAppliesCinemaScopeFromToken() throws Exception {
        loginAs(7, RoleEnum.CINEMA);
        doAnswer(invocation -> {
            invocation.getArgument(0, Record.class).setCinemaId(7);
            return null;
        }).when(recordService).applyCinemaScope(any(), eq("CINEMA"), eq(7));
        when(recordService.selectAll(any())).thenReturn(List.of(record(7)));

        mockMvc.perform(auth(get("/api/v1/records").param("cinemaId", "999")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].cinemaId").value(7));

        ArgumentCaptor<Record> captor = ArgumentCaptor.forClass(Record.class);
        verify(recordService).selectAll(captor.capture());
        assertThat(captor.getValue().getCinemaId()).isEqualTo(7);
    }

    /** 影厅列表同口径：影院角色由 token 限定本影院 */
    @Test
    void roomListAppliesCinemaScopeFromToken() throws Exception {
        loginAs(7, RoleEnum.CINEMA);
        doAnswer(invocation -> {
            invocation.getArgument(0, Room.class).setCinemaId(7);
            return null;
        }).when(roomService).applyCinemaScope(any(), eq("CINEMA"), eq(7));
        when(roomService.selectAll(any())).thenReturn(List.of(room(7)));

        mockMvc.perform(auth(get("/api/v1/rooms")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].cinemaId").value(7));

        verify(roomService).applyCinemaScope(any(), eq("CINEMA"), eq(7));
    }

    /** 用户列表：ADMIN/USER/CINEMA 的范围差异由 service 判断，控制器必须把身份原样传下去 */
    @Test
    void userListPassesScopedIdentity() throws Exception {
        loginAs(11, RoleEnum.USER);
        when(userService.listScoped(any(), eq("USER"), eq(11))).thenReturn(List.of());

        mockMvc.perform(auth(get("/api/v1/users")))
                .andExpect(status().isOk());

        verify(userService).listScoped(any(), eq("USER"), eq(11));
    }

    /** 建用户必须显式判 ADMIN：/users 前缀不在 ADMIN_WRITE_PREFIXES（用户要能改自己资料） */
    @Test
    void userAddRequiresAdmin() throws Exception {
        loginAs(11, RoleEnum.USER);
        mockMvc.perform(auth(post("/api/v1/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new-user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));
        verify(userService, never()).add(any());

        loginAs(1, RoleEnum.ADMIN);
        mockMvc.perform(auth(post("/api/v1/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new-user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
        verify(userService).add(any());
    }

    /** 影院账号建影厅：cinemaId 由控制器随 role/userId 一并交给 service 再归一 */
    @Test
    void roomWritePassesIdentityToService() throws Exception {
        loginAs(7, RoleEnum.CINEMA);

        mockMvc.perform(auth(post("/api/v1/rooms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cinemaId\":999,\"name\":\"1号厅\"}"))
                .andExpect(status().isOk());

        verify(roomService).addScoped(any(), eq("CINEMA"), eq(7));
    }

    /** 排片删除被订单挡住时，业务码必须透出 409 与可操作提示，而不是被改写成 500 */
    @Test
    void recordDeleteConflictKeepsBusinessCode() throws Exception {
        loginAs(1, RoleEnum.ADMIN);
        org.mockito.Mockito.doThrow(new CustomException(ErrorCode.BUSINESS_CONFLICT,
                        "该场次已有 2 笔订单，无法删除；如需下架请将放映状态改为「停售」"))
                .when(recordService).deleteScoped(5, "ADMIN", 1);

        mockMvc.perform(auth(delete("/api/v1/records/5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.BUSINESS_CONFLICT.code()))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已有 2 笔订单")));
    }

    /** 评价只属于 USER：影院/管理员账号即便 id 撞上也不能发表 */
    @Test
    void markAddRejectsNonUserRole() throws Exception {
        loginAs(7, RoleEnum.CINEMA);

        mockMvc.perform(auth(post("/api/v1/marks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filmId\":3,\"score\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));

        verify(markService, never()).add(any());
    }

    /** 发表评价：userId 取自 token，请求体里的 userId/id 一律被覆盖/清空 */
    @Test
    void markAddOverridesOwnerAndIdFromToken() throws Exception {
        loginAs(11, RoleEnum.USER);

        mockMvc.perform(auth(post("/api/v1/marks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":99,\"userId\":88,\"filmId\":3,\"score\":8.5,\"comment\":\"好看\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        ArgumentCaptor<Mark> captor = ArgumentCaptor.forClass(Mark.class);
        verify(markService).add(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getUserId()).isEqualTo(11);
        assertThat(captor.getValue().getFilmId()).isEqualTo(3);
    }

    /** 改别人的评价：403，且不能落到 update */
    @Test
    void markUpdateRejectsOtherUsersMark() throws Exception {
        loginAs(11, RoleEnum.USER);
        Mark other = new Mark();
        other.setId(5);
        other.setUserId(99);
        when(markService.selectById(5)).thenReturn(other);

        mockMvc.perform(auth(put("/api/v1/marks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":5,\"comment\":\"偷偷改\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));

        verify(markService, never()).update(any());
    }

    /** 点赞的 liked 是必填：缺失不能被默认成“取消点赞” */
    @Test
    void markLikeWithoutLikedIsParamInvalid() throws Exception {
        loginAs(11, RoleEnum.USER);

        mockMvc.perform(auth(put("/api/v1/marks/5/like"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()))
                .andExpect(jsonPath("$.msg").value("缺少 liked 参数"));
    }

    /** 评价列表按访问者身份带 liked/mine，viewerId 必须来自 token */
    @Test
    void markByFilmPassesViewerIdentity() throws Exception {
        loginAs(11, RoleEnum.USER);
        when(markService.listByFilm(3, 11, "USER", 1, 10)).thenReturn(null);

        mockMvc.perform(auth(get("/api/v1/marks/by-film").param("filmId", "3")))
                .andExpect(status().isOk());

        verify(markService).listByFilm(3, 11, "USER", 1, 10);
    }

    private static Record record(int cinemaId) {
        Record record = new Record();
        record.setId(1);
        record.setCinemaId(cinemaId);
        record.setTitle("测试影片");
        return record;
    }

    private static Room room(int cinemaId) {
        Room room = new Room();
        room.setId(1);
        room.setCinemaId(cinemaId);
        room.setName("1号厅");
        return room;
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", TOKEN);
    }
}
