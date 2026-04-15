package com.example.springboot.mapper;

import com.example.springboot.entity.Cinema;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CinemaMapper {

    List<Cinema> selectAll(Cinema cinema);

    @Select("select * from cinema where id = #{id}")
    Cinema selectById(Integer id);

    void insert(Cinema cinema);

    void updateById(Cinema cinema);

    void deleteById(Integer id);

    Cinema selectByUsername(String username);

    List<Cinema> selectByFilmId(@Param("cinema") Cinema cinema, @Param("filmId") Integer filmId);


}
