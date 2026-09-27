package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Mark;
import org.apache.ibatis.annotations.Param;

public interface MarkMapper extends BaseMapper<Mark> {

    /** 同一用户对同一影片是否已评价（评价唯一性由服务层据此拦截） */
    int countByUserAndFilm(@Param("userId") Integer userId, @Param("filmId") Integer filmId);
}
