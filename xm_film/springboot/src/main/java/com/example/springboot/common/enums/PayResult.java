package com.example.springboot.common.enums;

/**
 * 支付结果。
 *
 * 超时取消不能靠抛异常表达：payOrder 标注了 rollbackFor = Exception.class，
 * 抛出异常会把"取消订单"这次写入一并回滚，导致订单停留在待支付。
 * 因此超时分支改为返回本枚举，由控制器映射成业务错误码。
 */
public enum PayResult {
    /** 支付成功，订单流转为待取票 */
    PAID,
    /** 支付时已超时，订单已被取消（取消已落库） */
    TIMEOUT_CANCELLED
}
