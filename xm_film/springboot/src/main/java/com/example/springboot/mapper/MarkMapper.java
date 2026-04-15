package com.example.springboot.mapper;

import com.example.springboot.entity.Mark;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MarkMapper {

    List<Mark> selectAll(Mark mark);

    @Select("select * from mark where id = #{id}")
    Mark selectById(Integer id);

    void insert(Mark mark);

    void updateById(Mark mark);

    void deleteById(Integer id);


}
