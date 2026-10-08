package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.CinemaService;
import com.example.springboot.service.OrderedService;
import com.example.springboot.service.RecordService;
import com.example.springboot.service.RoomService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController extends BaseController<Room> {

    /** 影厅座位行列数的合法区间，与前端选座图渲染规模相称 */
    private static final int MIN_SEAT_DIMENSION = 1;
    private static final int MAX_SEAT_DIMENSION = 50;

    private final RoomService roomService;
    private final RecordService recordService;
    private final OrderedService orderedService;
    private final CinemaService cinemaService;

    public RoomController(RoomService roomService,
                          RecordService recordService,
                          OrderedService orderedService,
                          CinemaService cinemaService) {
        super(roomService);
        this.roomService = roomService;
        this.recordService = recordService;
        this.orderedService = orderedService;
        this.cinemaService = cinemaService;
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
        PageMethod.startPage(pageNum, pageSize);
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
        if (entity.getCinemaId() == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择所属影院");
        }
        validateSeatLayout(entity);
        // 影院名称由影院记录派生，不接受前端手填
        String title = cinemaTitleOf(entity.getCinemaId());
        if (title == null) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "所属影院不存在，无法确定影厅归属");
        }
        entity.setTitle(title);
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
        validateSeatLayout(entity);
        Integer cinemaId = entity.getCinemaId() != null ? entity.getCinemaId() : dbRoom.getCinemaId();
        String title = cinemaTitleOf(cinemaId);
        if (title != null) {
            entity.setTitle(title);
        }
        roomService.update(entity);
        return Result.success();
    }

    /** 影厅所属影院名称，由影院记录派生；影院不存在时返回 null，由调用方决定拒绝还是保持原值 */
    private String cinemaTitleOf(Integer cinemaId) {
        if (cinemaId == null) {
            return null;
        }
        Cinema cinema = cinemaService.selectById(cinemaId);
        return cinema == null ? null : cinema.getName();
    }

    private void validateSeatLayout(Room room) {
        validateSeatDimension(room.getSeatRows(), "座位行数");
        validateSeatDimension(room.getSeatCols(), "座位列数");
    }

    private void validateSeatDimension(Integer value, String label) {
        if (value != null && (value < MIN_SEAT_DIMENSION || value > MAX_SEAT_DIMENSION)) {
            throw new CustomException(ErrorCode.PARAM_INVALID,
                    label + "需在 " + MIN_SEAT_DIMENSION + "~" + MAX_SEAT_DIMENSION + " 之间");
        }
    }

    @Override
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Room room = roomService.selectById(id);
        ensureRoomAccess(room);
        ensureNoReferences(id);
        roomService.delete(id);
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        for (Integer id : ids) {
            ensureRoomAccess(roomService.selectById(id));
            ensureNoReferences(id);
        }
        roomService.deleteBatch(ids);
        return Result.success();
    }

    /** 影厅被排片或订单引用时禁止物理删除 */
    private void ensureNoReferences(Integer roomId) {
        int records = recordService.countByRoomId(roomId);
        int orders = orderedService.countByRoomId(roomId);
        if (records > 0 || orders > 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "该影厅已有 " + records + " 个排片、" + orders + " 笔订单，无法删除");
        }
    }

    private void applyCinemaScope(Room room) {
        if (isCinema()) {
            room.setCinemaId(currentUserId());
        }
    }

    private void ensureRoomAccess(Room room) {
        if (room == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "影厅不存在");
        }
        if (isAdmin()) {
            return;
        }
        if (isCinema()) {
            Integer cinemaId = currentUserId();
            if (cinemaId != null && cinemaId.equals(room.getCinemaId())) {
                return;
            }
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "无权操作该影厅");
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
