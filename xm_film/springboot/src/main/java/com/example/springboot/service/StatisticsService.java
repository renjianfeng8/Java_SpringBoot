package com.example.springboot.service;

import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台可视化大盘的统计口径：全部由数据库实时聚合，不落快照、不缓存、不造数。
 * 无数据的维度返回空列表，由前端渲染「暂无数据」占位。
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    /**
     * 趋势窗口：不含今天，只覆盖完整日。今天只过了一半，把它画进折线会让曲线
     * 在每个上午都呈现"断崖下跌"，读者会把半天数据误读成经营恶化。
     * 副作用是趋势末点正好是昨天，KPI 的环比可以直接从它推出来，后端不必另算。
     */
    static final int TREND_DAYS = 7;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String UNAUDITED = "未审核";
    private static final String PENDING_PICKUP = "待取票";

    @Resource
    private CinemaMapper cinemaMapper;

    @Resource
    private FilmMapper filmMapper;

    @Resource
    private OrderedMapper orderedMapper;

    @Resource
    private UserMapper userMapper;

    public Map<String, Object> overview() {
        List<Map<String, Object>> cinemaStatus = cinemaMapper.countGroupByStatus();
        Map<String, Object> today = orderedMapper.selectTodayPaidRevenue();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cinemaStatus", cinemaStatus);
        result.put("filmType", filmMapper.countGroupByType());
        // 三组分布同构：都是「维度值 → 计数」的原样分组，后端不派生、不排序、
        // 不补零，给什么维度值就是什么，由前端按自己的枚举补全并着色。
        result.put("orderStatus", orderedMapper.countGroupByStatus());
        // 统计时刻取自今日票房那次查询：同一次观测，页面上只有一个「今天」
        result.put("updatedAt", today.get("updatedAt"));
        result.put("summary", summary(today, cinemaStatus));
        result.put("revenueTrend", revenueTrend(TREND_DAYS));
        return result;
    }

    /**
     * 影院总数与待审核数由 cinemaStatus 派生 —— 同一份响应里已有的分组结果，
     * 不再发第二次 COUNT。重复取数会在两次数之间产生窗口，让两个数字对不上。
     */
    private Map<String, Object> summary(Map<String, Object> today, List<Map<String, Object>> cinemaStatus) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("todayRevenue", today.get("total"));
        summary.put("todayOrders", orderedMapper.countTodayPaidOrders());
        summary.put("totalUsers", userMapper.countAll());
        summary.put("totalCinemas", sumValues(cinemaStatus));
        summary.put("pendingCinemas", countOf(cinemaStatus, UNAUDITED));
        summary.put("pendingPickupOrders", orderedMapper.countByStatus(PENDING_PICKUP));
        return summary;
    }

    /** 近 days 个完整日（截至昨日）的逐日票房与订单数 */
    List<Map<String, Object>> revenueTrend(int days) {
        return revenueTrend(LocalDate.now(), days);
    }

    /**
     * 抽出 endExclusive 重载是为了可测：让用例钉死日期，不受运行时刻跨零点影响。
     */
    List<Map<String, Object>> revenueTrend(LocalDate endExclusive, int days) {
        LocalDate start = endExclusive.minusDays(days);

        Map<String, Map<String, Object>> byDate = new LinkedHashMap<>();
        for (Map<String, Object> row : orderedMapper.selectPaidRevenueByDay(start, endExclusive)) {
            byDate.put(String.valueOf(row.get("date")), row);
        }

        List<Map<String, Object>> trend = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            String date = start.plusDays(i).format(DAY);
            Map<String, Object> row = byDate.get(date);

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date);
            point.put("revenue", row == null ? BigDecimal.ZERO : row.get("revenue"));
            point.put("orders", row == null ? 0 : row.get("orders"));
            trend.add(point);
        }
        return trend;
    }

    private static long sumValues(List<Map<String, Object>> rows) {
        long total = 0L;
        for (Map<String, Object> row : rows) {
            Object value = row.get("value");
            if (value instanceof Number number) {
                total += number.longValue();
            }
        }
        return total;
    }

    private static long countOf(List<Map<String, Object>> rows, String name) {
        for (Map<String, Object> row : rows) {
            if (name.equals(row.get("name"))) {
                Object value = row.get("value");
                return value instanceof Number number ? number.longValue() : 0L;
            }
        }
        return 0L;
    }
}
