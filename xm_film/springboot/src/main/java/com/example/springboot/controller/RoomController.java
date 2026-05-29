package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Room;
import com.example.springboot.service.RoomService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "影厅管理", description = "放映厅 CRUD")
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController extends BaseController<Room> {
    public RoomController(RoomService roomService) {
        super(roomService);
    }
}
