package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.dto.response.FilmTypeJoinView;
import com.example.springboot.dto.response.NameValueView;
import com.example.springboot.entity.Film;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FilmMapper extends BaseMapper<Film> {

    List<Film> selectByTitle(String title);

    List<Film> selectByCinema(@Param("cinemaId") Integer cinemaId, @Param("filmId") Integer filmId);

    List<Film> selectBoxOfficeTop(Integer topNum);

    /** 评分榜：只含真正有评价的影片（SQL 内含 EXISTS(mark) 谓词） */
    List<Film> selectMarkTop(Integer topNum);

    /** 按 mark 评价均分回写影片评分；film.score 的唯一写者，无评价时写 NULL */
    void recalculateScore(Integer filmId);

    List<FilmTypeJoinView> selectFilmTypeJoin(List<Integer> filmIds);

    /** 大盘统计：影片类型分布（[{name, value}]，由 film_type 实时计数） */
    List<NameValueView> countGroupByType();

    void insertFilmTypes(@Param("filmId") Integer filmId, @Param("typeIds") List<Integer> typeIds);

    void deleteFilmTypesByFilmId(Integer filmId);
}
