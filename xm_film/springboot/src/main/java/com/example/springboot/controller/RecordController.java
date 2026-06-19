package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Record;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.RecordService;
import com.example.springboot.service.RoomService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Tag(name = "放映记录管理", description = "排片/放映场次 CRUD")
@RestController
@RequestMapping("/api/v1/records")
public class RecordController extends BaseController<Record> {
    private final RecordService recordService;
    private final RoomService roomService;

    public RecordController(RecordService recordService, RoomService roomService) {
        super(recordService);
        this.recordService = recordService;
        this.roomService = roomService;
    }

    @Override
    @GetMapping
    public Result list(Record entity) {
        applyCinemaScope(entity);
        return Result.success(recordService.selectAll(entity));
    }

    @Override
    @GetMapping("/page")
    public Result page(Record entity,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        applyCinemaScope(entity);
        PageHelper.startPage(pageNum, pageSize);
        return Result.success(new PageInfo<>(recordService.selectAll(entity)));
    }

    @Override
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Record record = recordService.selectById(id);
        if (record == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "排片不存在");
        }
        return Result.success(record);
    }

    @Override
    @PostMapping
    public Result add(@RequestBody Record entity) {
        if (isCinema()) {
            entity.setCinemaId(currentUserId());
        }
        requireAdminOrCinema();
        ensureRoomBelongsToCinema(entity);
        recordService.add(entity);
        return Result.success();
    }

    @Override
    @PutMapping
    public Result update(@RequestBody Record entity) {
        Record dbRecord = recordService.selectById(entity.getId());
        ensureRecordAccess(dbRecord);
        if (isCinema()) {
            entity.setCinemaId(currentUserId());
        }
        ensureRoomBelongsToCinema(entity);
        recordService.update(entity);
        return Result.success();
    }

    @Override
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Record record = recordService.selectById(id);
        ensureRecordAccess(record);
        recordService.delete(id);
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        for (Integer id : ids) {
            ensureRecordAccess(recordService.selectById(id));
        }
        recordService.deleteBatch(ids);
        return Result.success();
    }

    private void applyCinemaScope(Record record) {
        if (isCinema()) {
            record.setCinemaId(currentUserId());
        }
    }

    private void ensureRecordAccess(Record record) {
        if (record == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "排片不存在");
        }
        if (isAdmin()) {
            return;
        }
        if (isCinema() && currentUserId().equals(record.getCinemaId())) {
            return;
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "无权操作该排片");
    }

    private void ensureRoomBelongsToCinema(Record record) {
        if (record.getRoomId() == null || record.getCinemaId() == null) {
            return;
        }
        Room room = roomService.selectById(record.getRoomId());
        if (room == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "影厅不存在");
        }
        if (!record.getCinemaId().equals(room.getCinemaId())) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "影厅不属于当前影院");
        }
    }

    private void requireAdminOrCinema() {
        if (!isAdmin() && !isCinema()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
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
