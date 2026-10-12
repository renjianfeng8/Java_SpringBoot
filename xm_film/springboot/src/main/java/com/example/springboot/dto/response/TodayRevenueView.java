package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 今日票房：今天支付的售票收入合计（元）+ 统计时刻。
 * 两个消费者：前台公开只读端点 {@code GET /api/v1/films/box-office/today}，
 * 与后台大盘 {@code GET /api/v1/statistics/overview} 的 {@code summary.todayRevenue}。
 * 日期边界与统计时刻都取自数据库时钟，口径见 OrderedMapper.selectTodayPaidRevenue。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodayRevenueView {

    /** 今日收入合计（元），空集时 COALESCE 兜成 0 */
    private BigDecimal total;

    /** 统计时刻（"yyyy-MM-dd HH:mm:ss"） */
    private String updatedAt;
}
