package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Ordered;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderedMapper extends BaseMapper<Ordered> {

    List<Ordered> selectActiveByRecordId(Integer recordId);

    int countSeatInUse(@Param("recordId") Integer recordId, @Param("seat") String seat);

    Ordered selectByIdForUpdate(Integer id);

    List<Ordered> selectExpiredPendingOrders();

    int batchCancelExpiredOrders(@Param("ids") List<Integer> ids);

    /* 删除守卫引用计数：被订单引用的影片/影院/影厅/场次/用户不允许物理删除 */

    int countByFilmId(Integer filmId);

    int countByCinemaId(Integer cinemaId);

    int countByRoomId(Integer roomId);

    int countByRecordId(Integer recordId);

    int countByUserId(Integer userId);
}
