package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.OrderStatusCriteria;
import com.example.springboot.dto.response.DailyRevenueView;
import com.example.springboot.dto.response.NameValueView;
import com.example.springboot.dto.response.TodayRevenueView;
import com.example.springboot.entity.Ordered;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderedMapper extends BaseMapper<Ordered> {

    List<Ordered> selectActiveByRecordId(@Param("recordId") Integer recordId,
                                         @Param("s") OrderStatusCriteria criteria);

    /**
     * 今日票房：今天支付的售票收入合计（元）+ 统计时刻。
     * 两个消费者：前台公开只读端点 {@code GET /api/v1/films/box-office/today}，
     * 与后台大盘 {@code GET /api/v1/statistics/overview} 的 {@code summary.todayRevenue}。
     * 两处共用这一份实现 —— 「今日票房」只有一个口径（见 CLAUDE.md 的口径来源表）。
     */
    TodayRevenueView selectTodayPaidRevenue();

    /** 今日已支付订单数（笔）。口径与 selectTodayPaidRevenue 同源（共用 paidOrderStatuses 片段） */
    int countTodayPaidOrders();

    /**
     * 逐日已支付票房与订单数（[{date, revenue, orders}]），覆盖 [from, to)。
     * 无销售的日期不产生行，由调用方补零。
     */
    List<DailyRevenueView> selectPaidRevenueByDay(@Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    /** 按状态计数（大盘的「待取票订单」等） */
    int countByStatus(String status);

    /**
     * 大盘统计：订单状态分布（全部状态，不限于「已支付」）。
     * 与 countByStatus 的区别是一次取回五种状态，避免同一个页面对同一张表发五次 COUNT
     * —— 五次采样各有自己的时刻，加起来可能不等于总数。
     */
    List<NameValueView> countGroupByStatus();

    /**
     * 单座位占用 COUNT：生产下单已改为 selectActiveByRecordId 一次取回，
     * 本方法保留给 seatActiveCondition 的 SQL 回归测试。
     */
    int countSeatInUse(@Param("recordId") Integer recordId,
                       @Param("seat") String seat,
                       @Param("s") OrderStatusCriteria criteria);

    Ordered selectByIdForUpdate(Integer id);

    /** 取票大厅按码取单（唯一索引命中一行），用于判断该码当前属于哪种情形 */
    Ordered selectByPickupCode(String pickupCode);

    /**
     * 凭码核销的落库动作，返回受影响行数。条件里带 {@code status = '待取票'} 是关键：
     * 并发的两次核销只有一个能改到行，另一个拿到 0 行 —— 不靠先读后写，因此不需要
     * 悲观锁也不会各出一张票。与余额扣减的 {@code UPDATE ... WHERE balance >= ?} 同一手法。
     *
     * <p>状态取值由 {@code criteria} 带入，不在 XML 里写中文字面量。
     */
    int markPickedUpByCode(@Param("pickupCode") String pickupCode,
                           @Param("pickedUp") String pickedUp,
                           @Param("pending") String pending);

    /** 评价门禁：该用户对该影片是否有已取票订单（已取票是终态，一旦成立不会被推翻） */
    int countPickedUpByUserAndFilm(@Param("userId") Integer userId, @Param("filmId") Integer filmId,
                                   @Param("s") OrderStatusCriteria criteria);

    List<Ordered> selectExpiredPendingOrders(@Param("s") OrderStatusCriteria criteria);

    /**
     * 单笔订单是否已过支付期限（BL-05）。与 {@link #selectExpiredPendingOrders} 共用
     * {@code pendingExpiredCondition} 片段，只看数据库时钟。
     */
    int countExpiredById(@Param("id") Integer id, @Param("s") OrderStatusCriteria criteria);

    int batchCancelExpiredOrders(@Param("list") List<Integer> ids, @Param("s") OrderStatusCriteria criteria);

    /* 删除守卫引用计数：被订单引用的影片/影院/影厅/场次/用户不允许物理删除 */

    int countByFilmId(Integer filmId);

    int countByCinemaId(Integer cinemaId);

    int countByRoomId(Integer roomId);

    int countByRecordId(Integer recordId);

    int countByUserId(Integer userId);
}
