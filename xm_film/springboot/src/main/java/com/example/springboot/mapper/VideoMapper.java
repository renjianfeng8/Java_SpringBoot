package com.example.springboot.mapper;

import com.example.springboot.entity.Video;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface VideoMapper {

    List<Video> selectAll(Video video);

    @Select("select * from video where id = #{id}")
    Video selectById(Integer id);

    void insert(Video video);

    void updateById(Video video);

    void deleteById(Integer id);


}
