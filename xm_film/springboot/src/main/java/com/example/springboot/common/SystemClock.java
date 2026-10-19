package com.example.springboot.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 系统时钟的唯一读取点（JVM 时间）。
 *
 * <p><b>它与数据库时钟的关系</b>：两者<b>不是</b>可以互换的。
 * <ul>
 *   <li><b>业务判定</b>（订单超时、退款截止）必须用<b>数据库时钟</b>，
 *       因为比较是发生在 SQL 里的，引用同一份时钟才不会偶发不一致 —— 见 BL-05，
 *       那里 JVM 与 DB 相差整整 8 小时会导致超时永不命中；</li>
 *   <li><b>写给人看的</b>字段（下单时间、充值时间）与服务端日历（单号的日期段）
 *       用本类即可：它们是展示/运营信息，跟随部署地日历反而更直观。</li>
 * </ul>
 * 把这两类混为一谈，正是 BL-05 的根因。所以本类<b>只</b>提供展示用的时间，
 * 名字也刻意不叫 {@code Clock}（那个名字容易让人以为它也能用于超时判定）。
 *
 * <p><b>为什么格式也在这里</b>：{@link #nowFormatted()} 与
 * {@link DateTimeFormats#DATE_TIME} 是同一个约定的两半。调用方写
 * {@code nowFormatted()} 就不可能再出现"时间对了但格式写错"。
 */
public final class SystemClock {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private SystemClock() {}

    /** 当前时间，按系统默认时区，格式见 {@link DateTimeFormats#DATE_TIME} */
    public static String nowFormatted() {
        return LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormats.DATE_TIME);
    }

    /** 当前日期，{@code yyyyMMdd}（单号的日期段） */
    public static String todayCompact() {
        return LocalDate.now(ZoneId.systemDefault()).format(DAY);
    }
}
