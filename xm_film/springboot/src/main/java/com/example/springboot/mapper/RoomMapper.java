package com.example.springboot.mapper;

import com.example.springboot.entity.Room;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoomMapper {

    List<Room> selectAll(Room room);

    @Select("select * from room where id = #{id}")
    Room selectById(Integer id);

    void insert(Room room);

    void updateById(Room room);

    void deleteById(Integer id);


}
