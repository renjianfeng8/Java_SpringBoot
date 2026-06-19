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
}
