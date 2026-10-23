package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 资金流水响应视图，字段与实体 FundFlow 一致。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundFlowView {
    private Integer id;
    private Integer userId;
    private String userName;
    private String source;
    private BigDecimal changeAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Integer relatedId;
    private String createTime;
}
