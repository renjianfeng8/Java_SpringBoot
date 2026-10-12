package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 资金流水请求参数，字段与实体 FundFlow 一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundFlowRequest {
    private Integer id;
    private Integer userId;
    private String source;
    private BigDecimal changeAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Integer relatedId;
    private String createTime;
}
