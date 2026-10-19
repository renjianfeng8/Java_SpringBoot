package com.example.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实双连接并发守卫：{@code SELECT ... FOR UPDATE} 确实互斥。
 *
 * <p><b>为什么需要本类</b>：仓库此前的并发结论全部来自手工实测，测试树里
 * <b>0 条</b>用两条连接验证锁的用例。{@code RecordOverlapLockTest} 只断言 SQL 文本里
 * 有 {@code FOR UPDATE}，那只证明"写对了关键字"，不证明"锁真的挡住了第二个事务"——
 * 调用方没在事务里、或语句被换掉，文本断言照样绿。
 *
 * <p><b>本类怎么验证</b>：绕开 Spring 的事务代理，直接拿两条<b>不同的物理连接</b>：
 * <ol>
 *   <li>A 开事务、对目标行 {@code SELECT ... FOR UPDATE}（用 performance_schema.data_locks
 *       确认真的拿到了 RECORD X 锁）；</li>
 *   <li>B 在<b>另一个线程</b>上对同一行发同样的加锁读；</li>
 *   <li>断言 B 在 300 ms 内<b>拿不到</b>；</li>
 *   <li>A 提交后 B 立刻拿到 —— 这一步把因果钉死：只断言"B 卡住"会把任何卡顿
 *       误判成"锁生效"，而"提交后立即放行"才证明它等的是 A 那把锁。</li>
 * </ol>
 *
 * <p><b>踩过的坑</b>：最初把 A/B 的连接都在主线程创建，B 却在线程池里执行 ——
 * B 拿到的连接被 Spring 测试上下文按线程绑定，行为不可预期（实测 B 立即返回、
 * 锁根本没生效）。现在两条连接各自在<b>执行它的那个线程</b>上创建和关闭。
 *
 * <p>测试自身把 {@code innodb_lock_wait_timeout} 缩短到 2 秒，避免用例挂 50 秒。
 * 所有自建数据在 {@code finally} 里删除：本类<b>不能</b>用类级 {@code @Transactional}——
 * 那样锁永远不提交，用不上"提交后放行"这一步。
 */
@SpringBootTest
@ActiveProfiles("ci")
class RowLockMutualExclusionTest {

    /** B 最多等多久：够区分"被挡住"与"没被挡住" */
    private static final long B_WAIT_MILLIS = 300;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lock-probe-b");
        t.setDaemon(true);
        return t;
    });

    @Test
    void cinemaRowLockBlocksSecondTransactionUntilCommit() throws Exception {
        int id = insertCinema("locks_cinema");
        try {
            assertRowLockBlocksUntilCommit("cinema", id);
        } finally {
            jdbcTemplate.update("DELETE FROM cinema WHERE id = ?", id);
        }
    }

    @Test
    void filmRowLockBlocksSecondTransactionUntilCommit() throws Exception {
        int id = insertFilm("锁测试影片");
        try {
            assertRowLockBlocksUntilCommit("film", id);
        } finally {
            jdbcTemplate.update("DELETE FROM film WHERE id = ?", id);
        }
    }

    @Test
    void roomRowLockBlocksSecondTransactionUntilCommit() throws Exception {
        int cinemaId = insertCinema("locks_room_cinema");
        int id = insertRoom(cinemaId, "锁测试厅");
        try {
            assertRowLockBlocksUntilCommit("room", id);
        } finally {
            jdbcTemplate.update("DELETE FROM room WHERE id = ?", id);
            jdbcTemplate.update("DELETE FROM cinema WHERE id = ?", cinemaId);
        }
    }

    @Test
    void recordRowLockBlocksSecondTransactionUntilCommit() throws Exception {
        int cinemaId = insertCinema("locks_record_cinema");
        int roomId = insertRoom(cinemaId, "锁测试厅2");
        int filmId = insertFilm("锁测试影片2");
        int id = insertRecord(cinemaId, roomId, filmId);
        try {
            assertRowLockBlocksUntilCommit("record", id);
        } finally {
            jdbcTemplate.update("DELETE FROM record WHERE id = ?", id);
            jdbcTemplate.update("DELETE FROM room WHERE id = ?", roomId);
            jdbcTemplate.update("DELETE FROM cinema WHERE id = ?", cinemaId);
            jdbcTemplate.update("DELETE FROM film WHERE id = ?", filmId);
        }
    }

    /**
     * 双连接验证：A 持有行锁时 B 必须等；A 提交后 B 应立即放行。
     *
     * @param table 受测表（只用于拼 SQL；列名统一 {@code id}）
     */
    private void assertRowLockBlocksUntilCommit(String table, int id) throws Exception {
        String lockSql = "SELECT id FROM " + table + " WHERE id = ? FOR UPDATE";

        // A 与 B 各自在"执行它的线程"上建连，避免 Spring 测试上下文按线程绑定的干扰
        try (Connection a = openConnection()) {
            a.setAutoCommit(false);
            lockRow(a, lockSql, id);
            assertThat(tableLockModes(a))
                    .as("%s(id=%d) 上应出现排他记录锁", table, id)
                    .anyMatch(mode -> mode.contains("X"));

            Future<Long> bFuture = executor.submit(() -> {
                try (Connection b = openConnection()) {
                    b.setAutoCommit(false);
                    long t0 = System.nanoTime();
                    lockRow(b, lockSql, id);
                    return (System.nanoTime() - t0) / 1_000_000;
                }
            });

            // B 不应在窗口内拿到锁
            try {
                Long waited = bFuture.get(B_WAIT_MILLIS, TimeUnit.MILLISECONDS);
                throw new AssertionError("A 持有 " + table + "(id=" + id + ") 的行锁时，B 仅等待 "
                        + waited + " ms 就拿到了锁 —— 锁没有生效");
            } catch (java.util.concurrent.TimeoutException expected) {
                // 这正是期望：B 被挡住了
            }

            // A 提交后 B 必须放行，并且用了足够长的时间（确实是等锁等出来的）
            a.commit();
            long bWaitedMs = bFuture.get(5, TimeUnit.SECONDS);
            assertThat(bWaitedMs)
                    .as("A 提交后 B 应立刻拿到锁（实测总共等了 %d ms）", bWaitedMs)
                    .isGreaterThanOrEqualTo(B_WAIT_MILLIS);
        }
    }

    /** 当前事务在本连接上持有的记录锁模式（performance_schema.data_locks） */
    private static java.util.List<String> tableLockModes(Connection c) throws SQLException {
        java.util.List<String> modes = new java.util.ArrayList<>();
        try (ResultSet rs = c.createStatement().executeQuery(
                "SELECT LOCK_MODE FROM performance_schema.data_locks WHERE LOCK_TYPE = 'RECORD'")) {
            while (rs.next()) {
                modes.add(rs.getString(1));
            }
        }
        return modes;
    }

    // ---------- helpers ----------

    private Connection openConnection() throws SQLException {
        Connection c = dataSource.getConnection();
        // 把等锁上限缩到 2 秒：B 若是真的在等，应该在 A 提交前就超时报错，
        // 而不是把测试拖到默认的 50 秒
        try (PreparedStatement ps = c.prepareStatement("SET SESSION innodb_lock_wait_timeout = 2")) {
            ps.execute();
        }
        return c;
    }

    private static void lockRow(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("行不存在：" + id);
                }
            }
        }
    }

    private int insertCinema(String username) {
        jdbcTemplate.update("INSERT INTO cinema (username, password, name, status) "
                + "VALUES (?, 'x', '锁测试影院', '已审核')", username);
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
                + "VALUES (?, '锁测试影院', ?, 8, 8)", cinemaId, name);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM room WHERE name = ? ORDER BY id DESC LIMIT 1", Integer.class, name);
    }

    private int insertRecord(int cinemaId, int roomId, int filmId) {
        String start = "2028-01-01 10:00:00";
        jdbcTemplate.update("INSERT INTO record (cinema_id, room_id, film_id, title, start, price, status) "
                + "VALUES (?, ?, ?, '锁测试影片2', ?, '30.00', '正常')", cinemaId, roomId, filmId, start);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM record WHERE room_id = ? AND start = ? ORDER BY id DESC LIMIT 1",
                Integer.class, roomId, start);
    }
}
