package com.example.springboot.mapper;

import com.example.springboot.entity.Film;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FilmMapper {

    List<Film> selectAll(Film film);

    Film selectById(Integer id);

    void insert(Film film);

    void updateById(Film film);

    void deleteById(Integer id);

    // 新增：根据电影名称模糊查询（返回匹配的所有电影列表）
    List<Film> selectByTitle(@Param("title") String title);


    List<Film> selectByCinema(@Param("cinemaId") Integer cinemaId, @Param("filmId") Integer filmId);

    List<Film> selectBoxOfficeTop(@Param("topNum") Integer topNum);

    List<Film> selectMarkTop(@Param("topNum") Integer topNum);
}
