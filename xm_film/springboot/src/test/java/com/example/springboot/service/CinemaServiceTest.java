package com.example.springboot.service;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CinemaServiceTest {

    @Mock
    private CinemaMapper cinemaMapper;

    @Mock
    private CinemaDirectoryMapper directoryMapper;

    @InjectMocks
    private CinemaService cinemaService;

    private Cinema mockCinema;

    @BeforeEach
    void setUp() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        mockCinema = new Cinema();
        mockCinema.setId(1);
        mockCinema.setUsername("asks");
        mockCinema.setPassword(encoder.encode("cinema123"));
        mockCinema.setName("测试影院");
        mockCinema.setRole("CINEMA");
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

        assertEquals("CINEMA", cinema.getRole());
        assertEquals("未审核", cinema.getStatus());
        assertNotNull(cinema.getPassword());
        assertNotEquals("pwd123", cinema.getPassword());
        verify(cinemaMapper).insert(cinema);
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

        cinemaService.selectAll(new Cinema(), false);

        // 管理端审核列表需要看到未审核影院，故该路径不得过滤
        verify(cinemaMapper).selectByFilmId(any(Cinema.class), isNull(), eq(false));
    }

    @Test
    void updateShouldClearPasswordAndRole() {
        Cinema cinema = new Cinema();
        cinema.setId(1);
        cinema.setPassword("should-be-cleared");
        cinema.setRole("should-be-cleared");

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

        int created = cinemaService.importFromDirectory(List.of(1, 2));

        assertEquals(1, created);
        ArgumentCaptor<Cinema> captor = ArgumentCaptor.forClass(Cinema.class);
        verify(cinemaMapper).insert(captor.capture());
        Cinema c = captor.getValue();
        assertEquals("amap_p2", c.getUsername());
        assertEquals("CINEMA", c.getRole());
        assertEquals("已审核", c.getStatus());
        assertEquals("B影院", c.getName());
        assertEquals("021-1", c.getPhone());
        assertEquals("addr2", c.getAddress());
        assertEquals(Integer.valueOf(2), c.getDirectoryId());
        assertNotNull(c.getPassword());
        assertNotEquals("cinema123", c.getPassword());
    }

    @Test
    void importFromDirectoryWithEmptyIdsShouldDoNothing() {
        assertEquals(0, cinemaService.importFromDirectory(List.of()));
        verifyNoInteractions(cinemaMapper, directoryMapper);
    }

    @Test
    void importFromDirectoryWhenAllAlreadyLinkedShouldCreateNone() {
        when(cinemaMapper.selectLinkedDirectoryIds(List.of(1, 2))).thenReturn(List.of(1, 2));
        CinemaDirectory row1 = new CinemaDirectory();
        row1.setId(1); row1.setPoiId("p1");
        when(directoryMapper.selectByIds(List.of(1, 2))).thenReturn(List.of(row1));

        assertEquals(0, cinemaService.importFromDirectory(List.of(1, 2)));
        verify(cinemaMapper, never()).insert(any(Cinema.class));
    }

    @Test
    void importFromDirectoryWithNullIdsShouldDoNothing() {
        assertEquals(0, cinemaService.importFromDirectory(null));
        verifyNoInteractions(cinemaMapper, directoryMapper);
    }
}
