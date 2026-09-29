package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ordered {
    private Integer id;
    private String orders;
    private Integer recordId;
    private Integer userId;
    private Integer filmId;
    private String img;
    private Integer cinemaId;
    private Integer roomId;
    private String appointment;
    /**
     * 必须用包装类型：updateById 里是 <if test="total != null">，
     * 原始类型 double 永远非 null，导致任何不带 total 的状态流转（支付/取票/取消）
     * 都会把订单金额写成 0.00。
     */
    private Double total;
    /** 下单时的单价快照，取自场次票价；场次改价不影响历史订单 */
    private BigDecimal unitPrice;
    private Integer number;
    private String status;
    private String start;
    private String seat;
    private String createTime;
    private String pendingTimeoutAt;
    private String payTime;
    private Double payAmount;
    private String refundTime;
    private Double refundAmount;
    /**
     * 取票码（支付成功时生成，一单一码）。可用性派生自 status，本身不带有效/失效标记：
     * 核销只接受 待取票，因此「用过即废 / 退票作废 / 未付款不出发」都由状态迁移保证。
     * 有效期到放映结束（start + 片长），同样不落库。
     */
    private String pickupCode;

    private String userName;
    private String filmName;
    private String cinemaName;
    private String roomName;
}
