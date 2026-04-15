package com.example.springboot.mapper;

import com.example.springboot.entity.Ordered;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface OrderedMapper {

    List<Ordered> selectAll(Ordered ordered);

    @Select("select * from ordered where id = #{id}")
    Ordered selectById(Integer id);

    void insert(Ordered ordered);

    void updateById(Ordered ordered);

    void deleteById(Integer id);


}
