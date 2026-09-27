package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RecordStatus;
import com.example.springboot.entity.Record;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.OrderedService;
import com.example.springboot.service.RecordService;
import com.example.springboot.service.RoomService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
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
    private final OrderedService orderedService;

    public RecordController(RecordService recordService,
                            RoomService roomService,
                            OrderedService orderedService) {
        super(recordService);
        this.recordService = recordService;
        this.roomService = roomService;
        this.orderedService = orderedService;
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
        PageMethod.startPage(pageNum, pageSize);
        return Result.success(new PageInfo<>(recordService.selectAll(entity)));
    }

    @Override
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Record recordItem = recordService.selectById(id);
        if (recordItem == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "排片不存在");
        }
        return Result.success(recordItem);
    }

    @Override
    @PostMapping
    public Result add(@RequestBody Record entity) {
        if (isCinema()) {
            entity.setCinemaId(currentUserId());
        }
        requireAdminOrCinema();
        entity.setStatus(entity.getStatus() == null
                ? RecordStatus.NORMAL
                : RecordService.normalizeStatus(entity.getStatus()));
        ensureRoomBelongsToCinema(entity);
        recordService.validateSchedule(entity, null);
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
        // 局部更新：未提交的字段沿用库内值，避免"仅停售"这类操作被整表校验挡住
        if (entity.getRoomId() == null) {
            entity.setRoomId(dbRecord.getRoomId());
        }
        if (entity.getFilmId() == null) {
            entity.setFilmId(dbRecord.getFilmId());
        }
        if (entity.getStart() == null) {
            entity.setStart(dbRecord.getStart());
        }
        if (entity.getPrice() == null) {
            entity.setPrice(dbRecord.getPrice());
        }
        entity.setStatus(entity.getStatus() == null
                ? dbRecord.getStatus()
                : RecordService.normalizeStatus(entity.getStatus()));
        ensureRoomBelongsToCinema(entity);
        recordService.validateSchedule(entity, dbRecord.getStart());
        recordService.update(entity);
        return Result.success();
    }

    @Override
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Record recordItem = recordService.selectById(id);
        ensureRecordAccess(recordItem);
        ensureNoOrders(id);
        recordService.delete(id);
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        for (Integer id : ids) {
            ensureRecordAccess(recordService.selectById(id));
            ensureNoOrders(id);
        }
        recordService.deleteBatch(ids);
        return Result.success();
    }

    /** 订单引用排片，物理删除会带走交易凭证 —— 已产生订单的场次只能停售 */
    private void ensureNoOrders(Integer recordId) {
        int count = orderedService.countByRecordId(recordId);
        if (count > 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "该场次已有 " + count + " 笔订单，无法删除；如需下架请将放映状态改为「停售」");
        }
    }

    private void applyCinemaScope(Record recordItem) {
        if (isCinema()) {
            recordItem.setCinemaId(currentUserId());
        }
    }

    private void ensureRecordAccess(Record recordItem) {
        if (recordItem == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "排片不存在");
        }
        if (isAdmin()) {
            return;
        }
        if (isCinema()) {
            Integer cinemaId = currentUserId();
            if (cinemaId != null && cinemaId.equals(recordItem.getCinemaId())) {
                return;
            }
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "无权操作该排片");
    }

    private void ensureRoomBelongsToCinema(Record recordItem) {
        if (recordItem.getRoomId() == null || recordItem.getCinemaId() == null) {
            return;
        }
        Room room = roomService.selectById(recordItem.getRoomId());
        if (room == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "影厅不存在");
        }
        if (!recordItem.getCinemaId().equals(room.getCinemaId())) {
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
