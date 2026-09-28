package com.example.springboot.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargeCreateRequest {
    /** 金额范围（>0、上限 50000）由 RechargeService 统一校验，边界校验与演示文案同源 */
    @NotNull(message = "充值金额不能为空")
    private BigDecimal amount;
}
