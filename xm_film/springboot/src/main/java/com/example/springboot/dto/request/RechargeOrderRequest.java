package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 充值单据请求参数，字段与实体 RechargeOrder 一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RechargeOrderRequest {
    private Integer id;
    private String rechargeNo;
    private Integer userId;
    private BigDecimal amount;
    private String status;
    private String createTime;
    private String finishTime;
    private String remark;
}
