package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Cinema;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CinemaMapper extends BaseMapper<Cinema> {

    Cinema selectByUsername(String username);

    List<Cinema> selectByFilmId(@Param("cinema") Cinema cinema, @Param("filmId") Integer filmId);

    void updatePassword(Cinema cinema);

}
