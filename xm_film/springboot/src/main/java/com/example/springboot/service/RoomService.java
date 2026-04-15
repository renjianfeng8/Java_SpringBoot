package com.example.springboot.service;

import com.example.springboot.entity.Room;
import com.example.springboot.mapper.RoomMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    @Resource
    private RoomMapper roomMapper;

    public List<Room> selectAll(Room room) {
        return roomMapper.selectAll(room);
    }

    public Room selectById(Integer id) {
        return roomMapper.selectById(id);

    }

    public List<Room> selectList(Room room) {
        System.out.println(room);
        return null;
    }

    public PageInfo<Room> selectPage( Room room, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Room> list = roomMapper.selectAll(room);
        return PageInfo.of(list);
    }

    public void add(Room room) {
        roomMapper.insert(room);
    }

    public void update(Room room) {
        roomMapper.updateById(room);
    }

    public void delete(Integer id) {
        roomMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.delete(id);
        }
    }

}
