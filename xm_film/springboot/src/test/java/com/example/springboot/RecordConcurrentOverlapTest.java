package com.example.springboot;

import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.Record;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.RecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BL-01 的业务级并发回归：同一影厅<b>并发提交重叠时段</b>，只能落一场。
 *
 * <p><b>与 {@code RecordOverlapLockTest} 的分工</b>：那个类断言 SQL 文本里有
 * {@code FOR UPDATE}（"写对了关键字"）；本类真的让<b>两个线程各自开事务</b>调用
 * {@link RecordService#addScoped}，是"锁真的挡住了第二个业务请求"。
 *
 * <p><b>缺陷原样</b>（2026-10-18 手工实测）：不锁影厅行时，两个管理员同时为同一影厅
 * 提交重叠时段，各自的 {@code countRoomOverlap} 都读到 0，随后双双 INSERT ——
 * 库里出现两场物理重叠的排片，并发请求双双返回 200。库端建不了「任意时段不重叠」
 * 的区间约束，所以这一层应用锁是**唯一防线**。
 *
 * <p>测试连跑多轮（每轮用全新的影厅），减少"恰好没撞上"的偶然通过。
 * 每轮至少有一个请求必须失败，且失败原因必须是「该影厅在此时间段已有排片」，
 * 而不是别的原因 —— 后者说明测的不是这把锁。
 */
@SpringBootTest
@ActiveProfiles("ci")
class RecordConcurrentOverlapTest {

    private static final int ROUNDS = 6;

    @Autowired
    private RecordService recordService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentOverlappingSchedulesForSameRoomLeaveExactlyOne() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                assertSingleWinnerInRound(pool, round);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private void assertSingleWinnerInRound(ExecutorService pool, int round) throws Exception {
        int cinemaId = insertCinema("overlap_cinema_" + round);
        int roomId = insertRoom(cinemaId, "并发厅" + round);
        int filmId = insertFilm("并发影片" + round);
        String start = "2029-03-0" + round + " 10:00:00";

        try {
            int countBefore = countRecords(roomId);

            CountDownLatch startGate = new CountDownLatch(1);
            AtomicInteger succeeded = new AtomicInteger();
            List<String> failures = new ArrayList<>();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        startGate.await(5, TimeUnit.SECONDS);
                        recordService.addScoped(buildRecord(cinemaId, roomId, filmId, start),
                                RoleEnum.ADMIN.name(), null);
                        succeeded.incrementAndGet();
                    } catch (CustomException e) {
                        synchronized (failures) {
                            failures.add(e.getMessage());
                        }
                    } catch (Exception e) {
                        synchronized (failures) {
                            failures.add("意外异常: " + e);
                        }
                    }
                }));
            }
            startGate.countDown();
            for (Future<?> f : futures) {
                f.get(20, TimeUnit.SECONDS);
            }

            int countAfter = countRecords(roomId);
            assertThat(countAfter - countBefore)
                    .as("第 %d 轮：同一影厅并发提交重叠时段，应只落一场（实际新增 %d）", round, countAfter - countBefore)
                    .isEqualTo(1);
            assertThat(succeeded.get())
                    .as("第 %d 轮：应恰好一个请求成功", round)
                    .isEqualTo(1);
            assertThat(failures)
                    .as("第 %d 轮：另一个请求应以「时段已有排片」失败，而不是别的错误", round)
                    .hasSize(1)
                    .allSatisfy(msg -> assertThat(msg).contains("已有排片"));
        } finally {
            jdbcTemplate.update("DELETE FROM record WHERE room_id = ?", roomId);
            jdbcTemplate.update("DELETE FROM room WHERE id = ?", roomId);
            jdbcTemplate.update("DELETE FROM cinema WHERE id = ?", cinemaId);
            jdbcTemplate.update("DELETE FROM film WHERE id = ?", filmId);
        }
    }

    // ---------- helpers ----------

    private Record buildRecord(int cinemaId, int roomId, int filmId, String start) {
        Record r = new Record();
        r.setCinemaId(cinemaId);
        r.setRoomId(roomId);
        r.setFilmId(filmId);
        r.setStart(start);
        r.setPrice("30.00");
        return r;
    }

    private int countRecords(int roomId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM record WHERE room_id = ?", Integer.class, roomId);
        return n == null ? 0 : n;
    }

    private int insertCinema(String username) {
        jdbcTemplate.update("INSERT INTO cinema (username, password, name, status) "
                + "VALUES (?, 'x', '并发测试影院', '已审核')", username);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cinema WHERE username = ? ORDER BY id DESC LIMIT 1", Integer.class, username);
    }

    private int insertFilm(String title) {
        jdbcTemplate.update("INSERT INTO film (title, time, status) VALUES (?, 90, '已上映')", title);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM film WHERE title = ? ORDER BY id DESC LIMIT 1", Integer.class, title);
    }

    private int insertRoom(int cinemaId, String name) {
        jdbcTemplate.update("INSERT INTO room (cinema_id, title, name, seat_rows, seat_cols) "
                + "VALUES (?, '并发测试影院', ?, 8, 8)", cinemaId, name);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM room WHERE name = ? ORDER BY id DESC LIMIT 1", Integer.class, name);
    }
}
