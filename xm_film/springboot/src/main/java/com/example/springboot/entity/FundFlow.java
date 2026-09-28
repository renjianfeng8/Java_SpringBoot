package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 资金流水。每一笔余额变动（充值/购票/退票）留一条，只增不改不删，
 * 因此没有对应的 update/delete 接口。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundFlow {
    private Integer id;
    private Integer userId;
    /** 业务来源，取值见 {@link com.example.springboot.common.enums.FundSource} */
    private String source;
    /** 变动金额：正为入账，负为出账 */
    private BigDecimal changeAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    /** 关联业务单据ID：充值单ID 或 订单ID */
    private Integer relatedId;
    private String createTime;
}
