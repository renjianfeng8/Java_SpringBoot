package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Cinema;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CinemaMapper extends BaseMapper<Cinema> {

    Cinema selectByUsername(String username);

    List<Cinema> selectByFilmId(@Param("cinema") Cinema cinema,
                                @Param("filmId") Integer filmId,
                                @Param("approvedOnly") Boolean approvedOnly);

    void updatePassword(Cinema cinema);

    /** 大盘统计：影院状态分布（[{name, value}]，由 cinema 实时计数） */
    List<Map<String, Object>> countGroupByStatus();

}
