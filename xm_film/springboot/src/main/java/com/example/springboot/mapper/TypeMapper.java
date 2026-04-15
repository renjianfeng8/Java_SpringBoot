package com.example.springboot.mapper;

import com.example.springboot.entity.Type;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TypeMapper {

    List<Type> selectAll(Type type);

    @Select("select * from type where id = #{id}")
    Type selectById(Integer id);

    void insert(Type type);

    void updateById(Type type);

    void deleteById(Integer id);


}
