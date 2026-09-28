package com.example.springboot.dto.response;

import lombok.Data;

/**
 * 选座图上的单个占用视角。
 *
 * 选座图只需要两件事：这个座位被占、以及是不是我自己的。他人订单的订单号、
 * 用户 ID、金额一概不出现在响应里 —— 原先直接返回 {@code Ordered} 实体，
 * 等于把该场次所有订单的明细发给任意登录用户。
 */
@Data
public class SeatOccupancy {

    /** 被占用的座位号，任何登录用户都需要它来渲染占用图 */
    private String seat;

    /** 是否当前登录用户本人的订单 */
    private boolean mine;

    /* 以下字段仅本人订单才有值，供"继续支付 / 取消锁座"使用 */

    private Integer orderId;
    private String orders;
    private String status;
    private Double total;
    private String pendingTimeoutAt;
}
