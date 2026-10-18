package com.example.springboot;

import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.mapper.RoomMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BL-01 的绑定层守卫：排片冲突检测必须是<strong>当前读</strong>。
 *
 * <p>背景（2026-10-18 实测）：调用方在 REPEATABLE-READ 下先 {@code SELECT room ... FOR UPDATE}，
 * 再调 {@code countRoomOverlap}。只要这条查重是普通 SELECT，它读的就是事务开始时的快照 ——
 * 等锁期间对方刚提交的排片它看不见，两个并发请求都能读到 0、双双插入。
 * 当时实测真的落了两行重叠排片，并发请求双双返回 200。
 *
 * <p>本类钉住三件事：
 * <ol>
 *   <li>锁定读语句合法且语义正确（能看见本事务的行、不重叠时为零）；</li>
 *   <li>影厅行锁可用；</li>
 *   <li><b>查重语句必须含 FOR UPDATE</b> —— 这是防回退的关键断言：有人把它"简化"回
 *       {@code SELECT COUNT(*) ...} 时，本类立刻变红，而那正是复现缺陷的写法。</li>
 * </ol>
 *
 * <p>类级 {@code @Transactional}：自建数据随用例回滚，ci 库始终为空，无需 finally 清理。
 */
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class RecordOverlapLockTest {

    private static final String START = "2027-06-01 10:00:00";
    private static final String END = "2027-06-01 12:00:00";

    @Autowired
    RecordMapper recordMapper;

    @Autowired
    RoomMapper roomMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SqlSessionFactory sqlSessionFactory;

    /** 锁定读必须能看见本事务刚插入的行，且聚合套子查询的写法可执行 */
    @Test
    void countRoomOverlapSeesOwnRows() {
        int roomId = insertRoomAndRecord(START).roomId();

        assertThat(recordMapper.countRoomOverlap(roomId, null, START, END)).isEqualTo(1);
    }

    /** 时刻不重叠时不得误判 */
    @Test
    void nonOverlappingWindowHasNoConflict() {
        int roomId = insertRoomAndRecord(START).roomId();

        assertThat(recordMapper.countRoomOverlap(
                roomId, null, "2027-06-01 13:00:00", "2027-06-01 15:00:00")).isZero();
    }

    /**
     * 关键回归：查重必须是锁定读（当前读）。
     * 从 MyBatis 的 MappedStatement 取 SQL 模板断言含 FOR UPDATE。
     */
    @Test
    void countRoomOverlapUsesLockingRead() {
        String sql = sqlSessionFactory.getConfiguration()
                .getMappedStatement(
                        "com.example.springboot.mapper.RecordMapper.countRoomOverlap")
                .getBoundSql(null)
                .getSql();

        assertThat(sql.toUpperCase()).contains("FOR UPDATE");
    }

    /** 影厅行锁语句可执行；锁只在事务内有效，本类已处事务中 */
    @Test
    void roomRowCanBeLockedInsideTransaction() {
        int roomId = insertRoomAndRecord(START).roomId();

        assertThat(roomMapper.selectByIdForUpdate(roomId)).isNotNull();
        assertThat(roomMapper.selectByIdForUpdate(-1)).isNull();
    }

    // ---------- helpers ----------

    private record Fixture(int roomId, int filmId) {}

    private Fixture insertRoomAndRecord(String start) {
        // record.cinema_id / room.cinema_id 都受外键与 NOT NULL 约束，先建一个最小影院
        jdbcTemplate.update("INSERT INTO cinema (username, password, name, status) "
                + "VALUES ('test_cinema', 'x', '测试影院', '已审核')");
        Integer cinemaId = jdbcTemplate.queryForObject(
                "SELECT id FROM cinema WHERE username = 'test_cinema' ORDER BY id DESC LIMIT 1", Integer.class);

        jdbcTemplate.update("INSERT INTO room (cinema_id, title, name, seat_rows, seat_cols) "
                + "VALUES (?, '测试影院', '测试厅', 8, 8)", cinemaId);
        Integer roomId = jdbcTemplate.queryForObject(
                "SELECT id FROM room WHERE name = '测试厅' ORDER BY id DESC LIMIT 1", Integer.class);

        jdbcTemplate.update("INSERT INTO film (title, time, status) VALUES ('测试影片', 90, '已上映')");
        Integer filmId = jdbcTemplate.queryForObject(
                "SELECT id FROM film WHERE title = '测试影片' ORDER BY id DESC LIMIT 1", Integer.class);

        jdbcTemplate.update("INSERT INTO record (cinema_id, room_id, film_id, title, start, price, status) "
                + "VALUES (?, ?, ?, '测试影片', ?, '30.00', '正常')", cinemaId, roomId, filmId, start);

        return new Fixture(roomId, filmId);
    }
}
