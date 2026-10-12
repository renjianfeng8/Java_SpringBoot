package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 逐日已支付票房与订单数（{@code [{date, revenue, orders}]}），覆盖 [from, to)。
 * 无销售的日期不产生行，由调用方补零。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenueView {

    /** 日期（"yyyy-MM-dd"） */
    private String date;

    /** 当日收入合计（元） */
    private BigDecimal revenue;

    /** 当日订单数（笔） */
    private Long orders;
}
