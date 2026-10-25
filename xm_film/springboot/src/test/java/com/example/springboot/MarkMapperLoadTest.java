package com.example.springboot;

import com.example.springboot.dto.response.MarkView;
import com.example.springboot.mapper.MarkMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 评价列表的 likeCount / liked 从两条相关子查询改成 LEFT JOIN 后，
 * 必须仍然：同一条评价只出现一次、赞数正确、匿名访问者 liked=false。
 */
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class MarkMapperLoadTest {

    @Autowired
    private MarkMapper markMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void markViewAggregatesLikesWithoutDuplicatingRows() {
        String suffix = String.valueOf(System.nanoTime());
        int authorId = insertUser("mark-author-" + suffix);
        int viewerId = insertUser("mark-viewer-" + suffix);
        int filmId = insertFilm("mark-film-" + suffix);
        int markId = insertMark(authorId, filmId);
        jdbcTemplate.update("INSERT INTO mark_like (mark_id, user_id) VALUES (?, ?)", markId, authorId);
        jdbcTemplate.update("INSERT INTO mark_like (mark_id, user_id) VALUES (?, ?)", markId, viewerId);

        List<MarkView> marks = markMapper.selectFilmMarks(filmId, viewerId);
        assertThat(marks).hasSize(1);
        assertThat(marks.get(0).getLikeCount()).isEqualTo(2);
        assertThat(marks.get(0).getLiked()).isTrue();

        List<MarkView> anonymous = markMapper.selectFilmMarks(filmId, null);
        assertThat(anonymous).hasSize(1);
        assertThat(anonymous.get(0).getLikeCount()).isEqualTo(2);
        assertThat(anonymous.get(0).getLiked()).isFalse();
    }

    private int insertUser(String username) {
        jdbcTemplate.update("INSERT INTO user (username, password, role) VALUES (?, 'x', 'USER')", username);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    private int insertFilm(String title) {
        jdbcTemplate.update("INSERT INTO film (title) VALUES (?)", title);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    private int insertMark(int userId, int filmId) {
        jdbcTemplate.update("INSERT INTO mark (user_id, film_id, score) VALUES (?, ?, 8.0)", userId, filmId);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }
}
