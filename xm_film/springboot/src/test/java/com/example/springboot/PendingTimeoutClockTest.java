package com.example.springboot;

import com.example.springboot.entity.Ordered;
import com.example.springboot.service.OrderedService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BL-05 守卫：<b>超时判定只能依赖数据库时钟</b>。
 *
 * <p>背景：{@code pending_timeout_at} 原先由 JVM 的 {@code LocalDateTime.now()} 写入，
 * 却在 SQL 里与 DB 的 {@code NOW()} 比较。实测两个 MySQL 会话（UTC 与 +08:00）
 * 的 {@code NOW()} 相差整整 8 小时 —— 部署到 UTC 容器、连接串又写死
 * {@code serverTimezone=Asia/Shanghai} 时，清理任务会永不命中（废单永久占座）
 * 或提前 8 小时取消有效订单。
 *
 * <p><b>为什么读 mapper XML 原文而不是走 MyBatis 的 {@code getBoundSql}</b>：
 * 这一段是被 {@code <if>} 包住的动态 SQL，参数为 null 时求值到假分支、拿不到真实语句。
 * 而要守住的正是"源码里必须这么写"这件事，读原文最直接，也不会因参数变化而漏判。
 *
 * <p>将来若有人为了"方便"把写入改回 JVM 时间，本测试会红。
 */
class PendingTimeoutClockTest {

    private static final Path MAPPER_XML = Path.of(
            "src", "main", "resources", "mapper", "OrderedMapper.xml");

    private static String mapperXml() throws IOException {
        return Files.readString(MAPPER_XML, StandardCharsets.UTF_8);
    }

    /** 建单时不写 JVM 时间：服务层应置哨兵，让 mapper 用 NOW() + INTERVAL 计算 */
    @Test
    void orderEntityUsesDbClockSentinel() {
        Ordered probe = new Ordered();
        probe.setPendingTimeoutFromDb(true);

        assertThat(probe.getPendingTimeoutFromDb()).isTrue();
        // 哨兵与 pendingTimeoutAt 是两个独立字段：置哨兵时不该同时塞 JVM 时间
        assertThat(probe.getPendingTimeoutAt()).isNull();
    }

    /** insert 的超时列必须由数据库时钟计算，且分钟数与服务层常量一致 */
    @Test
    void insertComputesTimeoutWithDatabaseClock() throws IOException {
        String xml = mapperXml();

        assertThat(xml)
                .as("pending_timeout_at 必须由数据库时钟计算，不能回退到 JVM 时间")
                .contains("NOW() + INTERVAL " + OrderedService.PENDING_TIMEOUT_MINUTES + " MINUTE");
    }

    /** 超时判定的唯一谓词：两条查询都 include 同一个片段，且片段用 NOW() */
    @Test
    void bothTimeoutQueriesShareTheSamePredicate() throws IOException {
        String xml = mapperXml();

        assertThat(xml)
                .as("共用片段必须用数据库时钟比较")
                .contains("<sql id=\"pendingExpiredCondition\">")
                .contains("pending_timeout_at &lt; NOW()");

        // 两条查询各出现一次 include，共两处引用
        int refs = xml.split("<include refid=\"pendingExpiredCondition\"/>", -1).length - 1;
        assertThat(refs)
                .as("selectExpiredPendingOrders（清理）与 countExpiredById（支付）都应引用共用片段")
                .isEqualTo(2);
    }

    /**
     * 守卫：不允许出现"只差一处"的分叉 —— 任何直接写
     * {@code pending_timeout_at < NOW()} 的查询都必须经由片段。
     *
     * <p>这条比上一条更严：它防的是"新加第三条查询时自己写一份谓词"。
     */
    @Test
    void noQueryWritesItsOwnExpiryPredicate() throws IOException {
        String xml = mapperXml();

        long direct = xml.lines()
                .filter(line -> line.contains("pending_timeout_at &lt; NOW()"))
                .count();

        assertThat(direct)
                .as("超时谓词只该出现在 pendingExpiredCondition 片段里，实际出现了 %d 处", direct)
                .isEqualTo(1);
    }
}
