package com.example.springboot.common;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 代码质量专项抽出的公共组件守卫。
 *
 * <p>这些类不是"为了少写几行"而抽的，每一个都对应一条<b>只能有一处</b>的规则；
 * 本类钉住的是"规则本身"，而不是某个调用方的实现细节：
 * <ol>
 *   <li>{@link PriceParser} —— 票价什么叫合法（录排片与下单必须同口径）；</li>
 *   <li>{@link OrderNumber} —— 单号形态（订单号与充值单号共用一个生成器）；</li>
 *   <li>{@link PasswordMatcher} —— 什么算密码正确（4 个入口共用）；</li>
 *   <li>{@link SystemClock} / {@link DateTimeFormats} —— 落库时间形态。</li>
 * </ol>
 */
@SuppressWarnings("java:S5778") // 测试 lambda 内除被测调用外还有 helper 调用，规则只影响可读性
class SharedComponentTest {

    // ---------- PriceParser ----------

    /** 合法票价统一到两位小数，四舍五入 */
    @Test
    void priceIsNormalizedToTwoDecimals() {
        assertThat(PriceParser.parse("30")).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(PriceParser.parse("45.678")).isEqualByComparingTo(new BigDecimal("45.68"));
        assertThat(PriceParser.parse("0.005")).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    /**
     * 非法票价是<b>录入方</b>的错误，必须是 400（PARAM_INVALID），不能是 500。
     *
     * <p>行为变更记录：{@code OrderedService.parsePrice} 原先对 null/不可解析抛
     * {@code SYSTEM_ERROR}，而同一张排片在录入时走的是 {@code RecordService.parsePrice}
     * 的 {@code PARAM_INVALID}。同一件事两个错误码，是本次收敛要消掉的分裂。
     * 若有人把实现改回 SYSTEM_ERROR，本用例会红。
     *
     * <p>注意：下单侧当前<b>不可达</b>（price 列是 decimal），但录入侧可达，且两处
     * 现在共用同一实现 —— 谁先改回 500，这条都会红。
     */
    @Test
    void invalidPriceIsParamInvalidNotSystemError() {
        for (String bad : new String[]{null, "", "  ", "abc", "1e999999999"}) {
            assertThatThrownBy(() -> PriceParser.parse(bad))
                    .as("票价 %s", bad == null ? "null" : "\"" + bad + "\"")
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getCode())
                            .isEqualTo(ErrorCode.PARAM_INVALID.code()));
        }
    }

    /** 负数在解析层不拦 —— 那是业务校验（"票价必须大于 0"）的职责，不是格式问题 */
    @Test
    void priceParserDoesNotEnforceSign() {
        assertThat(PriceParser.parse("-1")).isEqualByComparingTo(new BigDecimal("-1.00"));
    }

    // ---------- OrderNumber ----------

    /**
     * 前缀是唯一差异：两种单号除前缀外都是"8 位日期 + 8 位大写十六进制"。
     * 这里用同一种判定写法分别断言，而不是拿一次随机结果的字面值去当正则
     * （后者必失败：生成的随机段当然不会恰好等于下一次生成的）。
     */
    @Test
    void orderNumberHasPrefixDateAndSuffix() {
        String plain = OrderNumber.generate("");
        assertThat(plain).matches("\\d{8}[0-9A-F]{8}");

        String recharge = OrderNumber.generate("R");
        assertThat(recharge).matches("R\\d{8}[0-9A-F]{8}");
        assertThat(recharge.substring(1)).matches("\\d{8}[0-9A-F]{8}");
    }

    /** 连续生成不重复（8 位十六进制段） */
    @Test
    void orderNumbersDoNotCollide() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 2000; i++) {
            assertThat(seen.add(OrderNumber.generate(""))).isTrue();
        }
    }

    // ---------- PasswordMatcher ----------

    private final PasswordEncoder encoder = PasswordMatcher.instance();

    /** BCrypt 存储 + 正确口令 → 通过；错误口令 → 拒绝 */
    @Test
    void bcryptRoundTrip() {
        String stored = PasswordMatcher.encode("s3cret");
        assertThat(PasswordMatcher.matches(encoder, "s3cret", stored)).isTrue();
        assertThat(PasswordMatcher.matches(encoder, "wrong", stored)).isFalse();
    }

    /**
     * 明文不再通过：存量明文由 {@code PlaintextPasswordMigration} 在启动时转成 BCrypt。
     *
     * <p>同时钉住 SEC-03 的原始攻击面：把库里存的 BCrypt 串本身当密码提交也必须失败，
     * 不能再靠 {@code raw.equals(stored)} 通过。
     */
    @Test
    void plaintextAndStoredHashNeverMatch() {
        String stored = PasswordMatcher.encode("s3cret");
        assertThat(PasswordMatcher.matches(encoder, "u1", "u1")).isFalse();
        assertThat(PasswordMatcher.matches(encoder, stored, stored)).isFalse();
        assertThat(PasswordMatcher.matches(encoder, "", "")).isFalse();
    }

    /**
     * 空值一律不匹配：提交空口令、或库里密码列为空，都不算通过。
     * 收敛前各处写法是 {@code raw.equals(stored)}，{@code raw} 为 null 时会直接 NPE。
     */
    @Test
    void nullValuesNeverMatch() {
        assertThat(PasswordMatcher.matches(encoder, null, "x")).isFalse();
        assertThat(PasswordMatcher.matches(encoder, "x", null)).isFalse();
        assertThat(PasswordMatcher.matches(encoder, null, null)).isFalse();
    }

    // ---------- SeatParser ----------

    private static Room room(int rows, int cols) {
        Room r = new Room();
        r.setSeatRows(rows);
        r.setSeatCols(cols);
        return r;
    }

    /**
     * <b>防双卖的核心</b>：{@code "1排01座"} 必须归一成 {@code "1排1座"}。
     *
     * <p>库里的座位是逗号串，占用检查走 {@code FIND_IN_SET}（逐字节比对），
     * 不做归一的话同一个座位能用 {@code "1排01座"} 再卖一次。
     *
     * <p><b>边界（既有行为，非本次引入）</b>：格式正则限定 1–2 位数字，
     * 所以 {@code "01排001座"}（两位数带前导零）会被判为格式错误而不是归一 ——
     * 前导零只可能出现在"单数字前补一个零"这个形态，见下一条断言。
     */
    @Test
    void seatWithLeadingZeroIsNormalizedToTheSameSeat() {
        assertThat(SeatParser.normalize("1排01座", room(8, 8))).containsExactly("1排1座");
        assertThat(SeatParser.normalize("01排1座", room(8, 8))).containsExactly("1排1座");
        // 归一后去重：同一座位写三遍只剩一个
        assertThat(SeatParser.normalize("1排1座,1排01座,01排1座", room(8, 8)))
                .containsExactly("1排1座");
        // 记录既有边界：三位数字形态直接拒绝，不会被"猜"成一个座位
        assertThatThrownBy(() -> SeatParser.normalize("01排001座", room(8, 8)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("座位格式错误");
    }

    /** 全角逗号、空格、前后空白都接受；多选保序 */
    @Test
    void separatorsAndWhitespaceAreTolerated() {
        assertThat(SeatParser.normalize(" 1排1座， 2排3座 ,3排1座 ", room(8, 8)))
                .containsExactly("1排1座", "2排3座", "3排1座");
    }

    /** 范围由影厅决定，不写死 8×8 */
    @Test
    void rangeFollowsRoomDimensions() {
        assertThat(SeatParser.normalize("12排20座", room(12, 20))).containsExactly("12排20座");

        assertThatThrownBy(() -> SeatParser.normalize("13排1座", room(12, 20)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("超出影厅范围");
        assertThatThrownBy(() -> SeatParser.normalize("1排21座", room(12, 20)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("超出影厅范围");
        assertThatThrownBy(() -> SeatParser.normalize("0排1座", room(12, 20)))
                .isInstanceOf(CustomException.class);
    }

    /** 影厅缺失或行列未配置时回退到 8×8（与 room 表默认值一致） */
    @Test
    void missingRoomFallsBackToDefaultDimensions() {
        assertThat(SeatParser.normalize("8排8座", null)).containsExactly("8排8座");
        assertThatThrownBy(() -> SeatParser.normalize("9排1座", null)).isInstanceOf(CustomException.class);
        assertThat(SeatParser.normalize("8排8座", room(8, 8))).containsExactly("8排8座");
    }

    /** 格式错误与空值：全部 PARAM_INVALID，不能漏成 500 */
    @Test
    void malformedInputIsParamInvalid() {
        for (String bad : new String[]{null, "", "   ", " , , ", "1排", "排1座", "1-1", "A排B座", "1排1座;"}) {
            assertThatThrownBy(() -> SeatParser.normalize(bad, room(8, 8)))
                    .as("输入 %s", bad == null ? "null" : "\"" + bad + "\"")
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getCode())
                            .isEqualTo(ErrorCode.PARAM_INVALID.code()));
        }
    }

    /** 单笔上限 6 个：6 个通过、7 个拒绝 */
    @Test
    void atMostSevenSeatsPerOrderIsRejectedAtSeven() {
        String six = "1排1座,1排2座,1排3座,1排4座,1排5座,1排6座";
        assertThat(SeatParser.normalize(six, room(8, 8))).hasSize(6);

        assertThatThrownBy(() -> SeatParser.normalize(six + ",1排7座", room(8, 8)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("最多选择 6 个座位");
    }

    // ---------- 时间 ----------
    /** 落库时间形态固定为 yyyy-MM-dd HH:mm:ss */
    @Test
    void nowFormattedUsesTheContractPattern() {
        assertThat(SystemClock.nowFormatted()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        assertThat(SystemClock.todayCompact()).matches("\\d{8}");
        assertThat(DateTimeFormats.DATE_TIME.format(java.time.LocalDateTime.of(2026, 1, 2, 3, 4, 5)))
                .isEqualTo("2026-01-02 03:04:05");
    }
}
