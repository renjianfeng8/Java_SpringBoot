package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 充值单据。提交申请只生成一条「处理中」单据，余额不变；
 * 只有模拟支付网关回调成功才置为「已完成」并入账。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RechargeOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String rechargeNo;
    private Integer userId;
    /**
     * 必须用包装类型：与 ordered.total 同因同治，凡被
     * <if test="x != null"> 守卫的字段一律不用原始类型。
     */
    private BigDecimal amount;
    private String status;
    private String createTime;
    private String finishTime;
    private String remark;
}
