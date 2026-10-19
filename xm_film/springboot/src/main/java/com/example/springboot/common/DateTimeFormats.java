package com.example.springboot.common;

import java.time.format.DateTimeFormatter;

/**
 * 系统内时间字符串的唯一格式出处。
 *
 * <p><b>为什么需要它</b>：{@code "yyyy-MM-dd HH:mm:ss"} 原先在 OrderedService、
 * RechargeService、RecordService、PayPasswordService 各定义一份，还有一处直接在调用点
 * 现拼 {@code DateTimeFormatter.ofPattern(...)}。这种格式不是"随便选的一个样式" ——
 * 它同时是<b>落库字符串</b>与<b>前后端契约</b>（DTO 注释、前端展示都按它解析）。
 * 只要有一处改成 {@code yyyy-MM-dd HH:mm}，那一条写入路径的秒位就永远为 00，
 * 或者同一秒内两条记录在读端变得无法比较。收敛成一处后，改格式只有一处可改。
 *
 * <p><b>为什么是常量而不是方法</b>：{@link DateTimeFormatter} 不可变且线程安全，
 * 直接复用同一实例即可，不需要每次 {@code ofPattern}。
 *
 * <p><b>与解析的关系</b>：本类只管<b>输出</b>。解析口径更宽容
 * （{@code RecordService.readStart} 接受只有日期、只有分钟、或 ISO 的 T 分隔），
 * 那是刻意的不对称 —— 存量数据形态不齐，但新写入必须统一。
 */
public final class DateTimeFormats {

    /** 库内与接口的时间形态：{@code yyyy-MM-dd HH:mm:ss} */
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeFormats() {}
}
