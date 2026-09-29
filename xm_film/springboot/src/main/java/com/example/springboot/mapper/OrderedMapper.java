package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Ordered;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface OrderedMapper extends BaseMapper<Ordered> {

    List<Ordered> selectActiveByRecordId(Integer recordId);

    /** 今日票房：今天支付的售票收入合计（元）+ 统计时刻，供前台公开只读接口使用 */
    Map<String, Object> selectTodayPaidRevenue();

    int countSeatInUse(@Param("recordId") Integer recordId, @Param("seat") String seat);

    Ordered selectByIdForUpdate(Integer id);

    /** 取票大厅按码取单（唯一索引命中一行），用于判断该码当前属于哪种情形 */
    Ordered selectByPickupCode(String pickupCode);

    /**
     * 凭码核销的落库动作，返回受影响行数。条件里带 {@code status = '待取票'} 是关键：
     * 并发的两次核销只有一个能改到行，另一个拿到 0 行 —— 不靠先读后写，因此不需要
     * 悲观锁也不会各出一张票。与余额扣减的 {@code UPDATE ... WHERE balance >= ?} 同一手法。
     */
    int markPickedUpByCode(String pickupCode);

    /** 评价门禁：该用户对该影片是否有已取票订单（已取票是终态，一旦成立不会被推翻） */
    int countPickedUpByUserAndFilm(@Param("userId") Integer userId, @Param("filmId") Integer filmId);

    List<Ordered> selectExpiredPendingOrders();

    int batchCancelExpiredOrders(@Param("ids") List<Integer> ids);

    /* 删除守卫引用计数：被订单引用的影片/影院/影厅/场次/用户不允许物理删除 */

    int countByFilmId(Integer filmId);

    int countByCinemaId(Integer cinemaId);

    int countByRoomId(Integer roomId);

    int countByRecordId(Integer recordId);

    int countByUserId(Integer userId);
}
