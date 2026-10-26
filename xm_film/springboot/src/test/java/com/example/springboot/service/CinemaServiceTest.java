package com.example.springboot.service;

import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.response.CinemaAccountImportView;
import com.example.springboot.dto.response.CinemaRegionItemView;
import com.example.springboot.dto.response.CinemaRegionView;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.example.springboot.mapper.CinemaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("java:S5778") // 测试 lambda 内除被测调用外还有 helper 调用，规则只影响可读性
class CinemaServiceTest {

    @Mock
    private CinemaMapper cinemaMapper;

    @Mock
    private CinemaDirectoryMapper directoryMapper;

    @InjectMocks
    private CinemaService cinemaService;

    private Cinema mockCinema;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        mockCinema = new Cinema();
        mockCinema.setId(1);
        mockCinema.setUsername("asks");
        mockCinema.setPassword(encoder.encode("cinema123"));
        mockCinema.setName("测试影院");
        mockCinema.setRole(RoleEnum.CINEMA.name());
        mockCinema.setStatus("已审核");
    }

    @Test
    void loginWithCorrectCredentialsShouldSucceed() {
        Account account = new Account();
        account.setUsername("asks");
        account.setPassword("cinema123");

        when(cinemaMapper.selectByUsername("asks")).thenReturn(mockCinema);

        Cinema result = cinemaService.login(account);

        assertNotNull(result);
        assertEquals("asks", result.getUsername());
    }

    @Test
    void loginWithNonExistentUsernameShouldThrow() {
        when(cinemaMapper.selectByUsername("ghost")).thenReturn(null);

        Account account = new Account();
        account.setUsername("ghost");
        account.setPassword("pwd");
        assertThrows(CustomException.class, () -> cinemaService.login(account));
    }

    @Test
    void loginWithWrongPasswordShouldThrow() {
        when(cinemaMapper.selectByUsername("asks")).thenReturn(mockCinema);

        Account account2 = new Account();
        account2.setUsername("asks");
        account2.setPassword("wrong-pwd");
        assertThrows(CustomException.class, () -> cinemaService.login(account2));
    }

    @Test
    void registerShouldCreateCinema() {
        Account account = new Account();
        account.setUsername("newcinema");
        account.setPassword("pwd123");
        when(cinemaMapper.selectByUsername("newcinema")).thenReturn(null);

        cinemaService.register(account);

        verify(cinemaMapper).insert(any(Cinema.class));
    }

    @Test
    void registerWithExistingUsernameShouldThrow() {
        when(cinemaMapper.selectByUsername("asks")).thenReturn(mockCinema);

        Account account3 = new Account();
        account3.setUsername("asks");
        account3.setPassword("pwd");
        assertThrows(CustomException.class, () -> cinemaService.register(account3));
        verify(cinemaMapper, never()).insert(any());
    }

    @Test
    void addShouldSetDefaultValues() {
        Cinema cinema = new Cinema();
        cinema.setUsername("newcinema");
        cinema.setPassword("pwd123");

        when(cinemaMapper.selectByUsername("newcinema")).thenReturn(null);

        cinemaService.add(cinema);

        assertEquals(RoleEnum.CINEMA.name(), cinema.getRole());
        assertEquals("未审核", cinema.getStatus());
        assertNotNull(cinema.getPassword());
        assertNotEquals("pwd123", cinema.getPassword());
        verify(cinemaMapper).insert(cinema);
    }

    @Test
    void addWithoutPasswordShouldBeRejected() {
        Cinema cinema = new Cinema();
        cinema.setUsername("no-password");
        cinema.setName("未设口令影院");

        when(cinemaMapper.selectByUsername("no-password")).thenReturn(null);

        assertThatThrownBy(() -> cinemaService.add(cinema))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("请设置影院的初始登录密码");
        verify(cinemaMapper, never()).insert(any(Cinema.class));
    }

    @Test
    void loginWithUnauditedCinemaShouldThrow() {
        mockCinema.setStatus("未审核");
        when(cinemaMapper.selectByUsername("asks")).thenReturn(mockCinema);

        Account account = new Account();
        account.setUsername("asks");
        account.setPassword("cinema123");

        CustomException ex = assertThrows(CustomException.class, () -> cinemaService.login(account));
        assertTrue(ex.getMsg().contains("审核"));
    }

    @Test
    void selectPageShouldForwardApprovedOnlyFlag() {
        when(cinemaMapper.selectByFilmId(any(Cinema.class), isNull(), eq(true)))
                .thenReturn(List.of(mockCinema));

        cinemaService.selectPage(new Cinema(), null, 1, 10, true);

        // 公开列表必须按「仅已审核」下推给 SQL，否则未审核影院会对外可见
        verify(cinemaMapper).selectByFilmId(any(Cinema.class), isNull(), eq(true));
    }

    @Test
    void selectAllShouldForwardApprovedOnlyFlag() {
        when(cinemaMapper.selectByFilmId(any(Cinema.class), isNull(), eq(false)))
                .thenReturn(List.of(mockCinema));

        cinemaService.selectAll(new Cinema(), null, false);

        // 管理端审核列表需要看到未审核影院，故该路径不得过滤
        verify(cinemaMapper).selectByFilmId(any(Cinema.class), isNull(), eq(false));
    }

    @Test
    void selectAllShouldForwardFilmId() {
        when(cinemaMapper.selectByFilmId(any(Cinema.class), eq(7), eq(false)))
                .thenReturn(List.of(mockCinema));

        cinemaService.selectAll(new Cinema(), 7, false);

        // filmId 决定 /cinemas 是否按上映影片筛，漏传即静默返回全部影院
        verify(cinemaMapper).selectByFilmId(any(Cinema.class), eq(7), eq(false));
    }

    @Test
    void updateShouldClearPasswordAndRole() {
        Cinema cinema = new Cinema();
        cinema.setId(1);
        cinema.setPassword("should-be-cleared");
        cinema.setRole("should-be-cleared");

        // 基类 update 先判存在（BL-07）：没有这一行，update 会在写库前抛 NOT_FOUND
        when(cinemaMapper.selectById(1)).thenReturn(mockCinema);

        cinemaService.update(cinema);

        assertNull(cinema.getPassword());
        assertNull(cinema.getRole());
        verify(cinemaMapper).updateById(cinema);
    }

    @Test
    void regionsShouldGroupDistrictsByCity() {
        when(cinemaMapper.selectRegions()).thenReturn(List.of(
                new CinemaRegionItemView("北京", "朝阳区"),
                new CinemaRegionItemView("北京", "海淀区"),
                new CinemaRegionItemView("上海", "浦东新区")));

        List<CinemaRegionView> regions = cinemaService.regions();

        assertEquals(2, regions.size());
        assertEquals("北京", regions.get(0).getCity());
        assertEquals(List.of("朝阳区", "海淀区"), regions.get(0).getDistricts());
        assertEquals("上海", regions.get(1).getCity());
        assertEquals(List.of("浦东新区"), regions.get(1).getDistricts());
    }

    @Test
    void regionsWithNullDistrictShouldYieldEmptyDistrictList() {
        when(cinemaMapper.selectRegions()).thenReturn(List.of(new CinemaRegionItemView("广州", null)));

        List<CinemaRegionView> regions = cinemaService.regions();

        assertEquals(1, regions.size());
        assertEquals("广州", regions.get(0).getCity());
        assertTrue(regions.get(0).getDistricts().isEmpty());
    }

    @Test
    void importFromDirectoryShouldSkipLinkedAndCreateRest() {
        CinemaDirectory row1 = new CinemaDirectory();
        row1.setId(1); row1.setPoiId("p1"); row1.setName("A影院");
        CinemaDirectory row2 = new CinemaDirectory();
        row2.setId(2); row2.setPoiId("p2"); row2.setName("B影院");
        row2.setPhone("021-1"); row2.setAddress("addr2");

        when(cinemaMapper.selectLinkedDirectoryIds(List.of(1, 2))).thenReturn(List.of(1));
        when(directoryMapper.selectByIds(List.of(1, 2))).thenReturn(List.of(row1, row2));

        CinemaAccountImportView result = cinemaService.importFromDirectory(List.of(1, 2));

        assertEquals(1, result.getCreated());
        ArgumentCaptor<Cinema> captor = ArgumentCaptor.forClass(Cinema.class);
        verify(cinemaMapper).insert(captor.capture());
        Cinema c = captor.getValue();
        assertEquals("amap_p2", c.getUsername());
        assertEquals(RoleEnum.CINEMA.name(), c.getRole());
        assertEquals("已审核", c.getStatus());
        assertEquals("B影院", c.getName());
        assertEquals("021-1", c.getPhone());
        assertEquals("addr2", c.getAddress());
        assertEquals(Integer.valueOf(2), c.getDirectoryId());
        assertNotNull(c.getPassword());
        assertNotEquals("cinema123", c.getPassword());

        // 初始口令只在响应里出现一次：必须与落库哈希匹配，且不是旧的公共口令
        assertEquals(1, result.getAccounts().size());
        CinemaAccountImportView.Account account = result.getAccounts().get(0);
        assertEquals("amap_p2", account.getUsername());
        assertNotEquals("cinema123", account.getInitialPassword());
        assertEquals(12, account.getInitialPassword().length());
        assertTrue(encoder.matches(account.getInitialPassword(), c.getPassword()),
                "返回的明文口令必须能匹配落库的 BCrypt 哈希");
    }

    /** 每个影院的初始口令必须不同：共用口令等于没有口令 */
    @Test
    void importFromDirectoryGeneratesDistinctPasswordPerCinema() {
        CinemaDirectory row1 = new CinemaDirectory();
        row1.setId(1); row1.setPoiId("p1"); row1.setName("A影院");
        CinemaDirectory row2 = new CinemaDirectory();
        row2.setId(2); row2.setPoiId("p2"); row2.setName("B影院");

        when(cinemaMapper.selectLinkedDirectoryIds(List.of(1, 2))).thenReturn(List.of());
        when(directoryMapper.selectByIds(List.of(1, 2))).thenReturn(List.of(row1, row2));

        CinemaAccountImportView result = cinemaService.importFromDirectory(List.of(1, 2));

        assertEquals(2, result.getCreated());
        assertEquals(2, result.getAccounts().size());
        assertNotEquals(result.getAccounts().get(0).getInitialPassword(),
                result.getAccounts().get(1).getInitialPassword(),
                "不同影院不得共享初始口令");
    }

    @Test
    void importFromDirectoryWithEmptyIdsShouldDoNothing() {
        assertEquals(0, cinemaService.importFromDirectory(List.of()).getCreated());
        verifyNoInteractions(cinemaMapper, directoryMapper);
    }

    @Test
    void importFromDirectoryWhenAllAlreadyLinkedShouldCreateNone() {
        when(cinemaMapper.selectLinkedDirectoryIds(List.of(1, 2))).thenReturn(List.of(1, 2));
        CinemaDirectory row1 = new CinemaDirectory();
        row1.setId(1); row1.setPoiId("p1");
        when(directoryMapper.selectByIds(List.of(1, 2))).thenReturn(List.of(row1));

        assertEquals(0, cinemaService.importFromDirectory(List.of(1, 2)).getCreated());
        verify(cinemaMapper, never()).insert(any(Cinema.class));
    }

    @Test
    void importFromDirectoryWithNullIdsShouldDoNothing() {
        assertEquals(0, cinemaService.importFromDirectory(null).getCreated());
        verifyNoInteractions(cinemaMapper, directoryMapper);
    }

    /**
     * BL-04：并发导入的败者在唯一索引上撞键，应<b>按幂等跳过</b>而不是抛 500。
     *
     * <p>实测（2026-10-19 修复前）：两个线程同时导入同一批 3 条名录，
     * 一个创建 3 家、另一个返回 500「系统错误」。
     */
    @Test
    void importFromDirectorySkipsConcurrentDuplicateInsteadOfFailing() {
        CinemaDirectory row1 = new CinemaDirectory();
        row1.setId(1); row1.setPoiId("p1"); row1.setName("A影院");
        CinemaDirectory row2 = new CinemaDirectory();
        row2.setId(2); row2.setPoiId("p2"); row2.setName("B影院");

        // 预查时两条都「未收录」—— 这正是并发窗口：另一个事务刚刚插进去
        when(cinemaMapper.selectLinkedDirectoryIds(List.of(1, 2))).thenReturn(List.of());
        when(directoryMapper.selectByIds(List.of(1, 2))).thenReturn(List.of(row1, row2));
        when(cinemaMapper.insert(any(Cinema.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry '1' for key 'cinema.uk_cinema_directory_id'"))
                .thenReturn(1);

        CinemaAccountImportView result = cinemaService.importFromDirectory(List.of(1, 2));

        assertEquals(1, result.getCreated(), "撞键的那条跳过，另一条照常创建");
        assertEquals(1, result.getAccounts().size(), "跳过的账号不得返回（它没有新建口令）");
        assertEquals("amap_p2", result.getAccounts().get(0).getUsername());
        verify(cinemaMapper, times(2)).insert(any(Cinema.class));
    }

    /**
     * BL-04 附带发现：并发导入实测报的是<b>死锁</b>（{@link PessimisticLockingFailureException}
     * 的一种），不是唯一键冲突 —— 两个事务以不同顺序插入、争同一把索引锁。
     *
     * <p>死锁是 MySQL 选一个事务回滚，属于「可重试」而非「用户操作错误」。
     * 必须给出可操作提示，而不是 500「系统错误」。
     */
    @Test
    void importFromDirectoryReportsDeadlockAsRetryableConflict() {
        CinemaDirectory row1 = new CinemaDirectory();
        row1.setId(1); row1.setPoiId("p1");

        when(cinemaMapper.selectLinkedDirectoryIds(List.of(1))).thenReturn(List.of());
        when(directoryMapper.selectByIds(List.of(1))).thenReturn(List.of(row1));
        when(cinemaMapper.insert(any(Cinema.class)))
                .thenThrow(new PessimisticLockingFailureException("Deadlock found when trying to get lock",
                        new RuntimeException("1213")));

        assertThatThrownBy(() -> cinemaService.importFromDirectory(List.of(1)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("正在导入同一批名录", "请稍后重试");
    }

    /**
     * BL-52：同一冲突的另一半形态是<b>等锁超时</b>（{@link CannotAcquireLockException}）。
     * 只捕死锁子类会让这条漏成 500，故实现必须捕父类型 {@link PessimisticLockingFailureException}。
     */
    @Test
    void importFromDirectoryReportsLockWaitTimeoutAsRetryableConflict() {
        CinemaDirectory row1 = new CinemaDirectory();
        row1.setId(1); row1.setPoiId("p1");

        when(cinemaMapper.selectLinkedDirectoryIds(List.of(1))).thenReturn(List.of());
        when(directoryMapper.selectByIds(List.of(1))).thenReturn(List.of(row1));
        when(cinemaMapper.insert(any(Cinema.class)))
                .thenThrow(new CannotAcquireLockException("Lock wait timeout exceeded",
                        new RuntimeException("1205")));

        assertThatThrownBy(() -> cinemaService.importFromDirectory(List.of(1)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("正在导入同一批名录", "请稍后重试");
    }
}
