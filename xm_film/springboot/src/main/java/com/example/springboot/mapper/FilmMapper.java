package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Film;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface FilmMapper extends BaseMapper<Film> {

    List<Film> selectByTitle(@Param("title") String title);

    List<Film> selectByCinema(@Param("cinemaId") Integer cinemaId, @Param("filmId") Integer filmId);

    List<Film> selectBoxOfficeTop(@Param("topNum") Integer topNum);

    List<Film> selectMarkTop(@Param("topNum") Integer topNum);

    /** 按 mark 评价均分回写影片评分；该影片无评价时不改动 */
    void recalculateScore(@Param("filmId") Integer filmId);

    List<Map<String, Object>> selectFilmTypeJoin(@Param("filmIds") List<Integer> filmIds);

    void insertFilmTypes(@Param("filmId") Integer filmId, @Param("typeIds") List<Integer> typeIds);

    void deleteFilmTypesByFilmId(@Param("filmId") Integer filmId);
}
