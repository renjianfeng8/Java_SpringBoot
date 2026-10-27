package com.example.springboot;

import com.example.springboot.common.enums.CinemaStatus;
import com.example.springboot.entity.Cinema;
import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.service.CinemaService;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理端影院列表必须能按审核状态筛选：首页「待审核」卡带 ?status=未审核 跳进列表，
 * 少了这条条件会把 87 家影院全列出来。
 */
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class CinemaStatusFilterLoadTest {

    private static final String APPROVED_USERNAME = "__cinema_filter_approved__";
    private static final String PENDING_USERNAME = "__cinema_filter_pending__";

    @Autowired
    CinemaService cinemaService;

    @Autowired
    CinemaMapper cinemaMapper;

    @Test
    void pageFiltersByStatus() {
        cinemaMapper.insert(cinema(APPROVED_USERNAME, CinemaStatus.APPROVED));
        cinemaMapper.insert(cinema(PENDING_USERNAME, CinemaStatus.UNAUDITED));

        Cinema query = new Cinema();
        query.setStatus(CinemaStatus.UNAUDITED);

        PageInfo<Cinema> page = cinemaService.selectPage(query, null, 1, 10, false);

        assertThat(page.getList())
                .extracting(Cinema::getUsername)
                .contains(PENDING_USERNAME)
                .doesNotContain(APPROVED_USERNAME);
    }

    private static Cinema cinema(String username, String status) {
        Cinema cinema = new Cinema();
        cinema.setUsername(username);
        cinema.setPassword("not-used");
        cinema.setName(username);
        cinema.setStatus(status);
        return cinema;
    }
}
