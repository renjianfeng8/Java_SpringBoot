package com.example.springboot.mapper;

import com.example.springboot.entity.Actor;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ActorMapper {

    List<Actor> selectAll(Actor actor);

    @Select("select * from actor where id = #{id}")
    Actor selectById(Integer id);

    void insert(Actor actor);

    void updateById(Actor actor);

    void deleteById(Integer id);


}
