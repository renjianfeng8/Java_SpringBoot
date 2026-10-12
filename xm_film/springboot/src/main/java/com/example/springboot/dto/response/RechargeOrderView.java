package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 充值单据响应视图，字段与实体 RechargeOrder 一致。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RechargeOrderView {
    private Integer id;
    private String rechargeNo;
    private Integer userId;
    private BigDecimal amount;
    private String status;
    private String createTime;
    private String finishTime;
    private String remark;
}
