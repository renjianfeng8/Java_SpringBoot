package com.example.springboot.common.enums;

public final class RechargeStatus {
    private RechargeStatus() {}

    /** 处理中 — 单据已创建，等待支付网关回调；此状态下余额不变 */
    public static final String PROCESSING = "处理中";

    /** 已完成 — 回调成功，充值金额已入账 */
    public static final String SUCCESS = "已完成";

    /** 已失败 — 回调失败，余额不变 */
    public static final String FAILED = "已失败";
}
