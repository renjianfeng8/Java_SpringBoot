package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Room;
import com.example.springboot.mapper.RoomMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 影厅的通用增删改查，外加按影院计数（统计与删除前置校验用）。
 */
@Service
@Transactional(readOnly = true)
public class RoomService extends BaseService<Room> {

    @Resource
    private RoomMapper roomMapper;

    @Override
    protected BaseMapper<Room> mapper() {
        return roomMapper;
    }

    public int countByCinemaId(Integer cinemaId) {
        return roomMapper.countByCinemaId(cinemaId);
    }
}
