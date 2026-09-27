package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private String userName;
    private String filmName;
    private String cinemaName;
    private String roomName;
}
