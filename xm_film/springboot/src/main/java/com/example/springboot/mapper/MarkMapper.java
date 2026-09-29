package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.dto.response.MarkView;
import com.example.springboot.entity.Mark;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MarkMapper extends BaseMapper<Mark> {

    /** 同一用户对同一影片是否已评价（评价唯一性由服务层据此拦截） */
    int countByUserAndFilm(@Param("userId") Integer userId, @Param("filmId") Integer filmId);

    /**
     * 某片的评价列表（投影 MarkView），按赞数降序 → id 降序。
     * viewerId 为 null（匿名/非 USER）时 liked / mine 一并为 false，无需 Java 侧分支。
     */
    List<MarkView> selectFilmMarks(@Param("filmId") Integer filmId, @Param("viewerId") Integer viewerId);

    /** 某人在某片上的评价（投影 MarkView），用于「写评价 / 修改评价」的分支；未评过返回 null */
    MarkView selectFilmMarkOfUser(@Param("filmId") Integer filmId, @Param("viewerId") Integer viewerId);
}
