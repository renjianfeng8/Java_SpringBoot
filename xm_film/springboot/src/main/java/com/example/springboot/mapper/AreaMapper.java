package com.example.springboot.mapper;

import com.example.springboot.entity.Area;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AreaMapper {

    List<Area> selectAll(Area area);

    @Select("select * from area where id = #{id}")
    Area selectById(Integer id);

    void insert(Area area);

    void updateById(Area area);

    void deleteById(Integer id);


}
