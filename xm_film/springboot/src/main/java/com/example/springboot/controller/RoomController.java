package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.dto.request.RoomRequest;
import com.example.springboot.dto.response.RoomView;
import com.example.springboot.entity.Room;
import com.example.springboot.service.RoomService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController extends BaseController<Room, RoomRequest, RoomView> {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        super(roomService);
        this.roomService = roomService;
    }

    @Override
    protected Room toEntity(RoomRequest request) {
        Room room = new Room();
        room.setId(request.getId());
        room.setCinemaId(request.getCinemaId());
        room.setTitle(request.getTitle());
        room.setName(request.getName());
        room.setSeatRows(request.getSeatRows());
        room.setSeatCols(request.getSeatCols());
        return room;
    }

    @Override
    protected RoomView toView(Room room) {
        RoomView view = new RoomView();
        view.setId(room.getId());
        view.setCinemaId(room.getCinemaId());
        view.setTitle(room.getTitle());
        view.setName(room.getName());
        view.setSeatRows(room.getSeatRows());
        view.setSeatCols(room.getSeatCols());
        return view;
    }

    @Override
    @GetMapping
    public Result<List<RoomView>> list(RoomRequest query) {
        limitList();
        Room entity = toEntity(query);
        roomService.applyCinemaScope(entity, AuthContext.role(), AuthContext.userId());
        return Result.success(roomService.selectAll(entity).stream().map(this::toView).toList());
    }

    @Override
    @GetMapping("/page")
    public Result<PageInfo<RoomView>> page(RoomRequest query,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        Room entity = toEntity(query);
        roomService.applyCinemaScope(entity, AuthContext.role(), AuthContext.userId());
        PageMethod.startPage(pageNum, pageSize);
        return Result.success(toPageView(new PageInfo<>(roomService.selectAll(entity))));
    }

    @Override
    @GetMapping("/{id}")
    public Result<RoomView> getById(@PathVariable Integer id) {
        Room room = roomService.selectByIdScoped(id, AuthContext.role(), AuthContext.userId());
        return Result.success(toView(room));
    }

    @Override
    @PostMapping
    public Result<Void> add(@RequestBody RoomRequest request) {
        roomService.addScoped(toEntity(request), AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @Override
    @PutMapping
    public Result<Void> update(@RequestBody RoomRequest request) {
        roomService.updateScoped(toEntity(request), AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @Override
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        roomService.deleteScoped(id, AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        roomService.deleteBatchScoped(ids, AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

}
