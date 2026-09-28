package com.example.springboot.common.enums;

/** 资金流水的业务来源。每一笔余额变动必须归入其中之一。 */
public final class FundSource {
    private FundSource() {}

    /** 充值 — 充值单据回调成功入账 */
    public static final String RECHARGE = "充值";

    /** 购票 — 支付待支付订单时扣款 */
    public static final String PURCHASE = "购票";

    /** 退票 — 已支付订单退票时退款入账 */
    public static final String REFUND = "退票";
}
