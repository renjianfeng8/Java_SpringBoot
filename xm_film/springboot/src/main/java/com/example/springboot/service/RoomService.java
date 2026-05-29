package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Room;
import com.example.springboot.mapper.RoomMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomService extends BaseService<Room> {

    @Resource
    private RoomMapper roomMapper;

    @Override
    protected BaseMapper<Room> mapper() {
        return roomMapper;
    }
}
