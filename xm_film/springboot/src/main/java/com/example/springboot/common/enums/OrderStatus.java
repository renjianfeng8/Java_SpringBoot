package com.example.springboot.common.enums;

public final class OrderStatus {
    private OrderStatus() {}

    /** 待支付 — 订单已创建，等待用户支付（5分钟超时） */
    public static final String PENDING_PAYMENT = "待支付";

    /** 待取票 — 订单已创建，等待用户取票 */
    public static final String PENDING = "待取票";

    /** 已取票 — 用户已完成取票 */
    public static final String PICKED_UP = "已取票";

    /** 已取消 — 订单已取消（主动取消或支付超时） */
    public static final String CANCELLED = "已取消";

    /** 已退票 — 已支付订单在放映前 60 分钟办理退票，座位释放、款项退回 */
    public static final String REFUNDED = "已退票";
}
