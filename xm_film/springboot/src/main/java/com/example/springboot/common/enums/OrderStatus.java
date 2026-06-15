package com.example.springboot.common.enums;

public final class OrderStatus {
    private OrderStatus() {}

    /** 待取票 — 订单已创建，等待用户取票 */
    public static final String PENDING = "待取票";

    /** 已取票 — 用户已完成取票 */
    public static final String PICKED_UP = "已取票";

    /** 已取消 — 订单已取消 */
    public static final String CANCELLED = "已取消";
}
