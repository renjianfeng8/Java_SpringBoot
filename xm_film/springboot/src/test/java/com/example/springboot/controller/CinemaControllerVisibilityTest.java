package com.example.springboot.controller;

import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.request.CinemaRequest;
import com.example.springboot.service.CinemaCoverService;
import com.example.springboot.service.CinemaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 前台影院列表不能出现未审核影院。影院/会员在前台也带 token，
 * 只按「非管理员」放行的写法会让已登录用户看到待审核影院。
 */
class CinemaControllerVisibilityTest {

    private final CinemaService cinemaService = mock(CinemaService.class);

    private CinemaController controller;

    @BeforeEach
    void setUp() {
        controller = new CinemaController(cinemaService, mock(CinemaCoverService.class));
        when(cinemaService.selectAll(any(), isNull(), Mockito.anyBoolean())).thenReturn(List.of());
        when(cinemaService.selectPage(any(), isNull(), anyInt(), anyInt(), Mockito.anyBoolean()))
                .thenReturn(new com.github.pagehelper.PageInfo<>(List.of()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void loginAs(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("role", role);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void listTreatsCinemaRoleAsPublic() {
        loginAs(RoleEnum.CINEMA.name());

        controller.list(new CinemaRequest());

        verify(cinemaService).selectAll(any(), isNull(), eq(true));
    }

    @Test
    void pageTreatsUserRoleAsPublic() {
        loginAs(RoleEnum.USER.name());

        controller.page(new CinemaRequest(), 1, 10);

        verify(cinemaService).selectPage(any(), isNull(), eq(1), eq(10), eq(true));
    }

    @Test
    void adminStillSeesUnapproved() {
        loginAs(RoleEnum.ADMIN.name());

        controller.list(new CinemaRequest());

        verify(cinemaService).selectAll(any(), isNull(), eq(false));
    }
}
