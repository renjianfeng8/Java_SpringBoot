package com.example.springboot;

import com.example.springboot.common.OrderStatusCriteria;
import com.example.springboot.common.enums.CinemaStatus;
import com.example.springboot.common.enums.OrderStatus;
import com.example.springboot.common.enums.RechargeStatus;
import com.example.springboot.common.enums.RecordStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 状态值守卫：<b>SQL 里的中文状态串必须与 Java 常量一致</b>。
 *
 * <p><b>为什么需要它</b>：状态列是 {@code varchar(20)}，库层不拒绝任何字符串。
 * 所以 {@code status = '待取票'} 与 {@code OrderStatus.PENDING} 是两份副本 ——
 * 改常量时编译器不报错、数据库也不报错，只是那条 SQL 从此匹配到 0 行，
 * 表现为"取票核销没反应""超时订单永远清不掉"，而日志里一切正常。
 *
 * <p><b>现状</b>：能传参的地方已经改为从 {@link OrderStatusCriteria} 取（见
 * {@code OrderedMapper}），剩下一处无法传参的：{@code paidOrderStatuses} 被
 * {@code FilmMapper.filmRevenueJoin} <b>跨 namespace 静态引用</b>，而 FilmMapper 的
 * 8 个查询方法都没有参数入口。本测试就负责钉住这一处。
 *
 * <p><b>它守的是"源码里必须这么写"</b>，所以直接读 XML 原文，而不是走 MyBatis 解析
 * （片段在跨 namespace include 时拿不到运行时 SQL）。
 */
class StatusLiteralGuardTest {

    private static final Path MAPPER_DIR = Path.of("src", "main", "resources", "mapper");

    /** 参数化来源与常量逐项一致 —— 它是 XML 里状态值的唯一入口 */
    @Test
    void criteriaMirrorsEnumsExactly() {
        OrderStatusCriteria c = OrderStatusCriteria.INSTANCE;
        assertThat(c.getPendingPayment()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(c.getPending()).isEqualTo(OrderStatus.PENDING);
        assertThat(c.getPickedUp()).isEqualTo(OrderStatus.PICKED_UP);
        assertThat(c.getCancelled()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(c.getRefunded()).isEqualTo(OrderStatus.REFUNDED);

        assertThat(c.getRecordNormal()).isEqualTo(RecordStatus.NORMAL);
        assertThat(c.getRecordStopped()).isEqualTo(RecordStatus.STOPPED);
        assertThat(c.getCinemaApproved()).isEqualTo(CinemaStatus.APPROVED);
        assertThat(c.getCinemaUnaudited()).isEqualTo(CinemaStatus.UNAUDITED);
        assertThat(c.getRechargeProcessing()).isEqualTo(RechargeStatus.PROCESSING);
        assertThat(c.getRechargeDone()).isEqualTo(RechargeStatus.SUCCESS);
        assertThat(c.getRechargeFailed()).isEqualTo(RechargeStatus.FAILED);
    }

    /**
     * 「已支付」片段必须恰好是 待取票 + 已取票。
     *
     * <p>这个集合的语义是"钱已经收到的订单"：待取票与已取票都已付款；
     * 待支付没付、已取消/已退票钱已退回。任何一处改动都会让票房少算或多算，
     * 而且不会有任何异常。
     */
    @Test
    void paidOrderStatusesFragmentMatchesPaidEnums() {
        String xml = read("OrderedMapper.xml");
        String fragment = extract(xml, "<sql id=\"paidOrderStatuses\">", "</sql>");

        assertThat(fragment)
                .as("paidOrderStatuses 片段必须只含 OrderStatus 里两种已支付状态")
                .contains("'" + OrderStatus.PENDING + "'")
                .contains("'" + OrderStatus.PICKED_UP + "'")
                .doesNotContain("'" + OrderStatus.PENDING_PAYMENT + "'")
                .doesNotContain("'" + OrderStatus.CANCELLED + "'")
                .doesNotContain("'" + OrderStatus.REFUNDED + "'");
    }

    /**
     * 全仓 mapper 里<b>不允许</b>再出现裸的订单状态字面量，唯一例外是上面那个片段。
     *
     * <p>这是"新增 SQL 时顺手写死中文"的防回退线：改了状态常量名或取值时，
     * 会在这里第一时间变红，而不是等线上发现某个功能默默失效。
     */
    @Test
    void noRawOrderStatusLiteralsOutsideThePaidFragment() throws IOException {
        String[] raw = {
                OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING, OrderStatus.PICKED_UP,
                OrderStatus.CANCELLED, OrderStatus.REFUNDED
        };
        try (Stream<Path> files = Files.list(MAPPER_DIR)) {
            files.filter(p -> p.toString().endsWith(".xml")).forEach(p -> {
                String name = p.getFileName().toString();
                String xml = read(name);
                // 去掉注释；paidOrderStatuses 片段只存在于 OrderedMapper.xml，只在那里剔除
                String stripped = xml.replaceAll("(?s)<!--.*?-->", "");
                if ("OrderedMapper.xml".equals(name)) {
                    stripped = stripped.replace(
                            extract(xml, "<sql id=\"paidOrderStatuses\">", "</sql>"), "");
                }
                for (String s : raw) {
                    assertThat(stripped)
                            .as("%s 里出现了裸状态字面量 '%s'，应改为 #{s.xxx}（见 OrderStatusCriteria）",
                                    name, s)
                            .doesNotContain("'" + s + "'");
                }
            });
        }
    }

    private static String read(String name) {
        try {
            return Files.readString(MAPPER_DIR.resolve(name), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(name, e);
        }
    }

    private static String extract(String text, String from, String to) {
        int start = text.indexOf(from);
        assertThat(start).as("找不到片段起点 %s", from).isNotNegative();
        int end = text.indexOf(to, start);
        assertThat(end).as("找不到片段终点 %s", to).isNotNegative();
        return text.substring(start, end + to.length());
    }

    /** 防止有人"为了让它绿"把断言写在变量上：确认上述常量确实各不相同 */
    @Test
    void statusValuesAreDistinct() {
        String[] all = {
                OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING, OrderStatus.PICKED_UP,
                OrderStatus.CANCELLED, OrderStatus.REFUNDED
        };
        assertThat(Arrays.stream(all).distinct().count()).isEqualTo(all.length);
    }
}
