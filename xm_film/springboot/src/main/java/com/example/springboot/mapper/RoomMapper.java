package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Room;

public interface RoomMapper extends BaseMapper<Room> {

    int countByCinemaId(Integer cinemaId);

    /**
     * 对影厅行加排它锁，返回该行（锁不存在行为 null）。
     *
     * 用途：排片的「查重叠 → 插入」是典型的先查后写。两个管理员并发为同一影厅
     * 提交重叠时段时，各自的 countRoomOverlap 都读到 0，随后双双 INSERT，
     * 库里就出现两场物理重叠的排片，而库端建不了「任意时段不重叠」的唯一约束
     * （那是区间约束，不是列约束）。故在查重之前先对<strong>影厅行</strong>加锁，
     * 让同一影厅的排片写入串行化 —— 锁的粒度选影厅而不是排片，因为要保护的
     * 正是「同一影厅内的所有排片」这一集合。
     */
    Room selectByIdForUpdate(Integer id);
}
