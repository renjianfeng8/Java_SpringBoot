package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 订单响应视图，字段与实体 Ordered 一致（含展示字段）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderedView {
    private Integer id;
    private String orders;
    private Integer recordId;
    private Integer userId;
    private Integer filmId;
    private String img;
    private Integer cinemaId;
    private Integer roomId;
    private String appointment;
    private Double total;
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
    private String pickupCode;
    private String userName;
    private String filmName;
    private String cinemaName;
    private String roomName;
}
