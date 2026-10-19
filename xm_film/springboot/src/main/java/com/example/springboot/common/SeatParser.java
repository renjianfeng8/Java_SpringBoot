package com.example.springboot.common;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 座位串的唯一解析口径：把"用户提交的座位文本"变成"库里存储的规范座位列表"。
 *
 * <p><b>为什么独立成组件</b>：这段逻辑同时承担三个彼此独立的规则 ——
 * <b>格式</b>（{行}排{列}座）、<b>范围</b>（不得超出该厅行列数）、
 * <b>规范化与去重</b>（{@code "1排01座"} 必须变成 {@code "1排1座"}）。
 * 它原先埋在 {@code OrderedService} 里，与订单状态机搅在一起；
 * 而"同一座位被双卖"这类事故的根因恰恰可能是规范化没做全
 * —— {@code countSeatInUse} 用的是逐字节的 {@code FIND_IN_SET}，
 * {@code "1排01座"} 与 {@code "1排1座"} 在库里就是两个不同的串。
 * 抽出来之后，这条规则可以被单独穷举测试，而不必拉起整个订单服务。
 *
 * <p><b>规范化的必要性（别再"简化"掉）</b>：若不去前导零、不统一全角逗号，
 * 同一座位能以一个"看起来一样"的字符串绕过占用检查。库里没有座位维度的唯一约束
 * （座位是逗号串），所以<b>这里是唯一防线</b>。
 *
 * <p><b>错误码一律 PARAM_INVALID</b>：座位是用户点出来的，格式/范围错误都是入参问题。
 * 缺省行列（{@code null}）回退到影厅表的列默认值，与库结构保持一致。
 */
public final class SeatParser {

    /** 座位号形状：{行}排{列}座 */
    private static final Pattern SEAT_SHAPE = Pattern.compile("(\\d{1,2})排(\\d{1,2})座");

    /** 影厅座位数缺失时的兜底，与 room 表列默认值一致 */
    public static final int DEFAULT_SEAT_ROWS = 8;
    public static final int DEFAULT_SEAT_COLS = 8;

    /** 单笔订单最多座位数，防止一次锁满整个影厅 */
    public static final int MAX_SEATS_PER_ORDER = 6;

    /** 座位分隔符：半角与全角逗号都接受（手机输入法常打全角） */
    private static final char GROUP_SEPARATOR = ',';
    private static final char FULLWIDTH_SEPARATOR = '，';

    private SeatParser() {}

    /**
     * 解析并规范化座位。
     *
     * @param rawSeats 用户提交的座位串，如 {@code "1排1座,2排3座"}
     * @param room     所属影厅，用于取行列上限；{@code null} 或行列缺失时用默认值
     * @return 规范化后的座位列表（保序、去重、不可变）
     * @throws CustomException {@code PARAM_INVALID}：为空、格式错、超范围、超数量
     */
    public static List<String> normalize(String rawSeats, Room room) {
        if (rawSeats == null || rawSeats.isBlank()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择座位");
        }
        List<String> rawTokens = Arrays.stream(rawSeats.replace(FULLWIDTH_SEPARATOR, GROUP_SEPARATOR)
                        .split(String.valueOf(GROUP_SEPARATOR)))
                .map(String::trim)
                .filter(seat -> !seat.isEmpty())
                .toList();
        if (rawTokens.isEmpty()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择座位");
        }

        int rows = room != null && room.getSeatRows() != null ? room.getSeatRows() : DEFAULT_SEAT_ROWS;
        int cols = room != null && room.getSeatCols() != null ? room.getSeatCols() : DEFAULT_SEAT_COLS;

        Set<String> seats = new LinkedHashSet<>();
        for (String raw : rawTokens) {
            Matcher matcher = SEAT_SHAPE.matcher(raw);
            if (!matcher.matches()) {
                throw new CustomException(ErrorCode.PARAM_INVALID, "座位格式错误: " + raw);
            }
            int row = Integer.parseInt(matcher.group(1));
            int col = Integer.parseInt(matcher.group(2));
            if (row < 1 || row > rows || col < 1 || col > cols) {
                throw new CustomException(ErrorCode.PARAM_INVALID,
                        "座位超出影厅范围: " + raw + "（本厅 " + rows + " 排 " + cols + " 座）");
            }
            seats.add(format(row, col));
        }
        if (seats.size() > MAX_SEATS_PER_ORDER) {
            throw new CustomException(ErrorCode.PARAM_INVALID,
                    "单笔订单最多选择 " + MAX_SEATS_PER_ORDER + " 个座位");
        }
        return List.copyOf(seats);
    }

    /** 规范座位形态：{@code R排C座}，无前导零 */
    public static String format(int row, int col) {
        return row + "排" + col + "座";
    }
}
