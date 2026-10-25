package com.example.springboot;

import com.example.springboot.entity.Film;
import com.example.springboot.entity.Record;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.RecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 年份/日期筛选从 LIKE '%…%' 改成范围后，边界必须仍然正确：
 * 2026 只命中 2026-01-01 ~ 2026-12-31，2026-10-25 只命中当天。
 */
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class DateFilterMapperLoadTest {

    @Autowired
    private FilmMapper filmMapper;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void filmYearFilterUsesHalfOpenRange() {
        String suffix = String.valueOf(System.nanoTime());
        String title2025 = "film-2025-" + suffix;
        String title2026 = "film-2026-" + suffix;
        String title2027 = "film-2027-" + suffix;
        jdbcTemplate.update("INSERT INTO film (title, start) VALUES (?, '2025-12-31')", title2025);
        jdbcTemplate.update("INSERT INTO film (title, start) VALUES (?, '2026-01-01')", title2026);
        jdbcTemplate.update("INSERT INTO film (title, start) VALUES (?, '2027-01-01')", title2027);

        Film query = new Film();
        query.setYear("2026");
        List<Film> films = filmMapper.selectAll(query);

        assertThat(films).extracting(Film::getTitle).contains(title2026);
        assertThat(films).extracting(Film::getTitle).doesNotContain(title2025, title2027);
    }

    @Test
    void recordDateFilterUsesHalfOpenDayRange() {
        String suffix = String.valueOf(System.nanoTime());
        jdbcTemplate.update("INSERT INTO cinema (username, password, name, status) VALUES (?, 'x', 'date-test', '已审核')",
                "date-cinema-" + suffix);
        int cinemaId = lastId();
        jdbcTemplate.update("INSERT INTO room (cinema_id, title, name) VALUES (?, 'date-room', 'date-room')", cinemaId);
        int roomId = lastId();
        jdbcTemplate.update("INSERT INTO film (title) VALUES (?)", "date-film-" + suffix);
        int filmId = lastId();
        String titleDay1 = "record-day1-" + suffix;
        String titleDay2 = "record-day2-" + suffix;
        jdbcTemplate.update("INSERT INTO record (cinema_id, room_id, film_id, title, start, price, status) "
                + "VALUES (?, ?, ?, ?, '2026-10-25 10:00:00', 10.00, '正常')", cinemaId, roomId, filmId, titleDay1);
        jdbcTemplate.update("INSERT INTO record (cinema_id, room_id, film_id, title, start, price, status) "
                + "VALUES (?, ?, ?, ?, '2026-10-26 10:00:00', 10.00, '正常')", cinemaId, roomId, filmId, titleDay2);

        Record query = new Record();
        query.setStart("2026-10-25");
        List<Record> records = recordMapper.selectAll(query);

        assertThat(records).extracting(Record::getTitle).contains(titleDay1);
        assertThat(records).extracting(Record::getTitle).doesNotContain(titleDay2);
    }

    private int lastId() {
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }
}
