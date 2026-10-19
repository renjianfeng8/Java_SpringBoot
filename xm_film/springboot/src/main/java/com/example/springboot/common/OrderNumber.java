package com.example.springboot.common;

import java.util.UUID;

/**
 * 业务单号的统一生成器（订单号、充值单号）。
 *
 * <p><b>为什么抽出来</b>：两个生成方法此前的实现逐字相同，只差前缀 ——
 * {@code 20261019 + UUID 前 8 位大写}。它们不是"巧合相同"，而是同一个业务规则：
 * 单号必须<b>按日期可读、按随机段防撞</b>。如果将来只改了订单号（比如把随机段从 8 位加长），
 * 充值单号会悄悄留在旧口径上，而单号通常被前端当作全局标识展示，口径分裂很难被发现。
 *
 * <p><b>为什么不做唯一性重试</b>：两个调用方都在事务内（建单、扣款），
 * 唯一键冲突会让事务进入 rollback-only，同一事务内无法重试。库里
 * {@code ordered.orders} / {@code recharge_order.recharge_no} 上的唯一索引是兜底，
 * 8 位十六进制段（16^8 ≈ 4.3e9）足以让碰撞概率可以忽略。
 *
 * <p><b>为什么日期取系统默认时区而不是数据库时间</b>：单号是给人看的运营编号，
 * 应跟随部署地的日历；与"超时判定必须用数据库时钟"不是一回事 ——
 * 后者是正确性问题（BL-05），前者只是可读性，差几毫秒无影响。
 */
public final class OrderNumber {

    /** 随机段长度。调整它同时影响两种单号，这正是抽公共类想要的效果。 */
    private static final int SUFFIX_LENGTH = 8;

    private OrderNumber() {}

    /**
     * 生成业务单号：{@code 前缀 + yyyyMMdd + 8 位大写十六进制随机段}。
     *
     * @param prefix 单号前缀，订单号为空串、充值单号为 {@code "R"}
     */
    public static String generate(String prefix) {
        String date = SystemClock.todayCompact();
        String suffix = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, SUFFIX_LENGTH)
                .toUpperCase();
        return prefix + date + suffix;
    }
}
