package com.example.springboot.service;

import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock private CinemaMapper cinemaMapper;
    @Mock private FilmMapper filmMapper;
    @Mock private OrderedMapper orderedMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks private StatisticsService statisticsService;

    private static Map<String, Object> grouped(String name, long value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("value", value);
        return row;
    }

    private static Map<String, Object> today(Object total, String updatedAt) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("total", total);
        row.put("updatedAt", updatedAt);
        return row;
    }

    private static Map<String, Object> day(String date, String revenue, long orders) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("date", date);
        row.put("revenue", new BigDecimal(revenue));
        row.put("orders", orders);
        return row;
    }

    /** 影院总数与待审核数必须从**同一份** cinemaStatus 派生，不另发一次 COUNT 查询 */
    @Test
    void overview_shouldDeriveCinemaCountsFromTheStatusGrouping() {
        when(cinemaMapper.countGroupByStatus())
                .thenReturn(List.of(grouped("已审核", 15L), grouped("未审核", 3L)));
        when(filmMapper.countGroupByType()).thenReturn(List.of());
        when(orderedMapper.selectTodayPaidRevenue())
                .thenReturn(today(new BigDecimal("12840.00"), "2026-10-07 14:32:10"));
        when(orderedMapper.countTodayPaidOrders()).thenReturn(96);
        when(orderedMapper.countByStatus("待取票")).thenReturn(12);
        when(orderedMapper.selectPaidRevenueByDay(any(), any())).thenReturn(List.of());
        when(userMapper.countAll()).thenReturn(1024);

        Map<String, Object> result = statisticsService.overview();

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals(18L, summary.get("totalCinemas"));
        assertEquals(3L, summary.get("pendingCinemas"));
        assertEquals(1024, summary.get("totalUsers"));
        assertEquals(96, summary.get("todayOrders"));
        assertEquals(12, summary.get("pendingPickupOrders"));
        assertEquals(new BigDecimal("12840.00"), summary.get("todayRevenue"));
        // 统计时刻与今日票房同源：页面上不会出现两个「今天」
        assertEquals("2026-10-07 14:32:10", result.get("updatedAt"));
    }

    /**
     * 订单状态分布原样透传：后端不排序、不补零、不按「已支付」过滤。
     * 后两条是这张卡能不能画出五种颜色的全部依据 —— 一旦有人给它套上
     * paidOrderStatuses（待取票 / 已取票），未支付 / 已取消 / 已退票就会静默消失。
     */
    @Test
    void overview_shouldPassOrderStatusGroupingThroughUnfiltered() {
        when(cinemaMapper.countGroupByStatus()).thenReturn(List.of());
        when(filmMapper.countGroupByType()).thenReturn(List.of());
        when(orderedMapper.selectTodayPaidRevenue()).thenReturn(today(BigDecimal.ZERO, "2026-10-07 14:32:10"));
        when(orderedMapper.countTodayPaidOrders()).thenReturn(0);
        when(orderedMapper.countByStatus("待取票")).thenReturn(0);
        when(orderedMapper.selectPaidRevenueByDay(any(), any())).thenReturn(List.of());
        when(userMapper.countAll()).thenReturn(0);
        when(orderedMapper.countGroupByStatus()).thenReturn(List.of(
                grouped("待支付", 8L),
                grouped("已取票", 61L),
                grouped("已取消", 4L),
                grouped("已退票", 2L)));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orderStatus =
                (List<Map<String, Object>>) statisticsService.overview().get("orderStatus");

        assertEquals(4, orderStatus.size());
        // 含未支付态的两种：说明没有被「已支付」口径过滤
        assertTrue(orderStatus.stream().anyMatch(row -> "待支付".equals(row.get("name"))));
        assertTrue(orderStatus.stream().anyMatch(row -> "已取消".equals(row.get("name"))));
        // 顺序是 mapper 给的顺序，服务层不重排 —— 排序属于展示层（图例按枚举补全）
        assertEquals("待支付", orderStatus.get(0).get("name"));
        assertEquals(61L, orderStatus.get(1).get("value"));
    }

    /** 没有销售的日子必须补成 0：折线图缺一天会连成一条跨越两天的直线 */
    @Test
    void revenueTrend_shouldFillDaysWithoutSalesWithZero() {
        LocalDate end = LocalDate.of(2026, 10, 7);   // 窗口 = 09-30 .. 10-06
        when(orderedMapper.selectPaidRevenueByDay(LocalDate.of(2026, 9, 30), end))
                .thenReturn(List.of(day("2026-10-01", "5000.00", 40L)));

        List<Map<String, Object>> trend = statisticsService.revenueTrend(end, 7);

        assertEquals(7, trend.size());
        assertEquals("2026-09-30", trend.get(0).get("date"));
        assertEquals(BigDecimal.ZERO, trend.get(0).get("revenue"));
        assertEquals(0, trend.get(0).get("orders"));
        assertEquals("2026-10-01", trend.get(1).get("date"));
        assertEquals(new BigDecimal("5000.00"), trend.get(1).get("revenue"));
        assertEquals(40L, trend.get(1).get("orders"));
        assertEquals("2026-10-06", trend.get(6).get("date"));
    }

    /** 窗口是 [今天-7, 今天)：今天只过了一半，画进折线会让曲线每天上午都"断崖下跌" */
    @Test
    void revenueTrend_shouldNotIncludeToday() {
        when(orderedMapper.selectPaidRevenueByDay(any(), any())).thenReturn(List.of());

        List<Map<String, Object>> trend = statisticsService.revenueTrend(LocalDate.of(2026, 10, 7), 7);

        assertTrue(trend.stream().noneMatch(p -> "2026-10-07".equals(p.get("date"))));
        assertEquals("2026-10-06", trend.get(trend.size() - 1).get("date"));
    }

    /** 影院表为空时不得抛异常：求和与取值都要在空列表上返回 0 */
    @Test
    void overview_withNoCinemas_shouldReportZeroCounts() {
        when(cinemaMapper.countGroupByStatus()).thenReturn(List.of());
        when(filmMapper.countGroupByType()).thenReturn(List.of());
        when(orderedMapper.selectTodayPaidRevenue()).thenReturn(today(BigDecimal.ZERO, "2026-10-07 14:32:10"));
        when(orderedMapper.countTodayPaidOrders()).thenReturn(0);
        when(orderedMapper.countByStatus("待取票")).thenReturn(0);
        when(orderedMapper.selectPaidRevenueByDay(any(), any())).thenReturn(List.of());
        when(userMapper.countAll()).thenReturn(0);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) statisticsService.overview().get("summary");

        assertEquals(0L, summary.get("totalCinemas"));
        assertEquals(0L, summary.get("pendingCinemas"));
    }
}
