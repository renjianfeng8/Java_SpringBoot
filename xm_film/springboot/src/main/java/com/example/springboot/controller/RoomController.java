package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.RoomService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Tag(name = "影厅管理", description = "放映厅 CRUD")
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController extends BaseController<Room> {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        super(roomService);
        this.roomService = roomService;
    }

    @Override
    @GetMapping
    public Result list(Room entity) {
        applyCinemaScope(entity);
        return Result.success(roomService.selectAll(entity));
    }

    @Override
    @GetMapping("/page")
    public Result page(Room entity,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        applyCinemaScope(entity);
        PageHelper.startPage(pageNum, pageSize);
        return Result.success(new PageInfo<>(roomService.selectAll(entity)));
    }

    @Override
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Room room = roomService.selectById(id);
        ensureRoomAccess(room);
        return Result.success(room);
    }

    @Override
    @PostMapping
    public Result add(@RequestBody Room entity) {
        if (isCinema()) {
            entity.setCinemaId(currentUserId());
        }
        requireAdminOrCinema();
        roomService.add(entity);
        return Result.success();
    }

    @Override
    @PutMapping
    public Result update(@RequestBody Room entity) {
        Room dbRoom = roomService.selectById(entity.getId());
        ensureRoomAccess(dbRoom);
        if (isCinema()) {
            entity.setCinemaId(currentUserId());
        }
        roomService.update(entity);
        return Result.success();
    }

    @Override
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Room room = roomService.selectById(id);
        ensureRoomAccess(room);
        roomService.delete(id);
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        for (Integer id : ids) {
            ensureRoomAccess(roomService.selectById(id));
        }
        roomService.deleteBatch(ids);
        return Result.success();
    }

    private void applyCinemaScope(Room room) {
        if (isCinema()) {
            room.setCinemaId(currentUserId());
        }
    }

    private void ensureRoomAccess(Room room) {
        if (room == null) {
            throw new CustomException("404", "影厅不存在");
        }
        if (isAdmin()) {
            return;
        }
        if (isCinema() && currentUserId().equals(room.getCinemaId())) {
            return;
        }
        throw new CustomException("403", "无权操作该影厅");
    }

    private void requireAdminOrCinema() {
        if (!isAdmin() && !isCinema()) {
            throw new CustomException("403", "权限不足");
        }
    }

    private boolean isAdmin() {
        return "ADMIN".equals(currentRole());
    }

    private boolean isCinema() {
        return "CINEMA".equals(currentRole());
    }

    private String currentRole() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : (String) request.getAttribute("role");
    }

    private Integer currentUserId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String userId = (String) request.getAttribute("userId");
        return userId == null ? null : Integer.valueOf(userId);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
