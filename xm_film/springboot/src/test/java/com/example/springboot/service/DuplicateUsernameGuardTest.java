package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Admin;
import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.AdminMapper;
import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.example.springboot.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * BL-03 守卫：用户名唯一索引撞键时，必须转成与串行路径同一句业务提示，而不是漏成 500。
 *
 * <p>背景：「先 selectByUsername 判空、后 insert」在并发下两个请求都可能读到 null。
 * 库中 {@code user}/{@code admin}/{@code cinema} 三表都已有 {@code username} 唯一索引
 * （见 {@code db/inventory.tsv}），所以不会双插——但撞键抛出的
 * {@link DuplicateKeyException} 原先无人处理，会落到 GlobalExceptionHandler 的兜底
 * 分支变成 500「系统错误」。
 *
 * <p><b>为什么不用并发 HTTP 复现</b>：实测 8 线程 × 5 轮、三种角色全部是
 * 「1 个 200 + 7 个 409」——本地单实例下 MySQL 的 INSERT 会阻塞同名并发插入，
 * 先查后插的窗口被自然串行化，无法通过 HTTP 稳定撞出撞键。因此这里直接让 mapper
 * 抛 {@link DuplicateKeyException}，模拟"窗口被撞上"的那一刻。
 *
 * <p>这条守护在多实例部署、或将来去掉先查后插改为直接插入时，就是唯一的正确性来源。
 */
@ExtendWith(MockitoExtension.class)
class DuplicateUsernameGuardTest {

    @Mock
    UserMapper userMapper;

    @Mock
    AdminMapper adminMapper;

    @Mock
    CinemaMapper cinemaMapper;

    @Mock
    CinemaDirectoryMapper cinemaDirectoryMapper;

    @Mock
    OrderedService orderedService;

    private UserService userService;
    private AdminService adminService;
    private CinemaService cinemaService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, orderedService);
        adminService = new AdminService(adminMapper);
        cinemaService = new CinemaService(cinemaMapper, cinemaDirectoryMapper);
    }

    /** 注册用户撞唯一键：转 409 + 「账号已存在」，不是 500 */
    @Test
    void duplicateUserUsernameBecomesBusinessConflict() {
        // 先查返回 null：模拟并发下"我读到这个名字还没人用"
        when(userMapper.selectByUsername("race")).thenReturn(null);
        when(userMapper.insert(any(User.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry 'race' for key 'username'"));

        assertThatThrownBy(() -> userService.register(account("race")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getCode()).isEqualTo(ErrorCode.BUSINESS_CONFLICT.code());
                    assertThat(ex.getMsg()).contains("账号已存在");
                });
    }

    /** 新增管理员撞唯一键：同一句提示 */
    @Test
    void duplicateAdminUsernameBecomesBusinessConflict() {
        when(adminMapper.selectByUsername("race")).thenReturn(null);
        when(adminMapper.insert(any(Admin.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry 'race' for key 'username'"));

        Admin admin = new Admin();
        admin.setUsername("race");
        admin.setPassword("pwd123");

        assertThatThrownBy(() -> adminService.add(admin))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("账号已存在");
    }

    /** 注册影院撞唯一键：同一句提示（名录导入生成的 amap_+poiId 也走这条） */
    @Test
    void duplicateCinemaUsernameBecomesBusinessConflict() {
        when(cinemaMapper.selectByUsername("amap_B0FF")).thenReturn(null);
        when(cinemaMapper.insert(any(Cinema.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry 'amap_B0FF' for key 'username'"));

        Cinema cinema = new Cinema();
        cinema.setUsername("amap_B0FF");
        cinema.setPassword("pwd123");

        assertThatThrownBy(() -> cinemaService.add(cinema))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("账号已存在");
    }

    /** 串行路径（先查已存在）仍走原守卫，两处提示必须一致 */
    @Test
    void serialDuplicateKeepsTheSameMessage() {
        User existing = new User();
        existing.setUsername("race");
        when(userMapper.selectByUsername("race")).thenReturn(existing);

        assertThatThrownBy(() -> userService.register(account("race")))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("账号已存在");
    }

    private Account account(String username) {
        Account a = new Account();
        a.setUsername(username);
        a.setPassword("pwd123");
        return a;
    }
}
