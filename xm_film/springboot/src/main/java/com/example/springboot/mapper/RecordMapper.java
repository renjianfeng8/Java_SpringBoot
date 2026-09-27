package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Record;
import org.apache.ibatis.annotations.Param;

public interface RecordMapper extends BaseMapper<Record> {

    Record selectByIdForUpdate(Integer id);

    /** 同一影厅内与 [start, end) 存在时段重叠的未停售场次数量；id 为编辑时的排除项 */
    int countRoomOverlap(@Param("roomId") Integer roomId,
                         @Param("id") Integer id,
                         @Param("start") String start,
                         @Param("end") String end);

    int countByFilmId(Integer filmId);

    int countByCinemaId(Integer cinemaId);

    int countByRoomId(Integer roomId);
}
