package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.RoomMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 影厅的通用增删改查，外加按影院计数（统计与删除前置校验用）；
 * 以及角色范围内的增删改查编排（归属校验、座位规模校验、影院名称派生、引用校验）。
 */
@Service
@Transactional(readOnly = true)
public class RoomService extends BaseService<Room> {

    /** 影厅座位行列数的合法区间，与前端选座图渲染规模相称 */
    private static final int MIN_SEAT_DIMENSION = 1;
    private static final int MAX_SEAT_DIMENSION = 50;

    private final RoomMapper roomMapper;

    private final RecordService recordService;

    private final OrderedService orderedService;

    private final CinemaService cinemaService;

    public RoomService(RoomMapper roomMapper, RecordService recordService,
                       OrderedService orderedService, CinemaService cinemaService) {
        this.roomMapper = roomMapper;
        this.recordService = recordService;
        this.orderedService = orderedService;
        this.cinemaService = cinemaService;
    }

    @Override
    protected BaseMapper<Room> mapper() {
        return roomMapper;
    }

    public int countByCinemaId(Integer cinemaId) {
        return roomMapper.countByCinemaId(cinemaId);
    }

    /** 列表/分页范围：影院角色只能看本影院影厅 */
    public void applyCinemaScope(Room room, String role, Integer userId) {
        if (RoleEnum.CINEMA.name().equals(role)) {
            room.setCinemaId(userId);
        }
    }

    /** 详情：校验归属后返回（不存在时由归属校验抛 NOT_FOUND） */
    public Room selectByIdScoped(Integer id, String role, Integer userId) {
        Room room = selectById(id);
        ensureRoomAccess(room, role, userId);
        return room;
    }

    /** 新增影厅：影院角色归属本影院，校验座位规模并从影院记录派生名称后落库 */
    @Transactional(rollbackFor = Exception.class)
    public void addScoped(Room room, String role, Integer userId) {
        if (RoleEnum.CINEMA.name().equals(role)) {
            room.setCinemaId(userId);
        }
        requireAdminOrCinema(role);
        if (room.getCinemaId() == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择所属影院");
        }
        validateSeatLayout(room);
        // 影院名称由影院记录派生，不接受前端手填
        String title = cinemaTitleOf(room.getCinemaId());
        if (title == null) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "所属影院不存在，无法确定影厅归属");
        }
        room.setTitle(title);
        add(room);
    }

    /** 编辑影厅：先校验归属，再校验座位规模，并重新派生影院名称 */
    @Transactional(rollbackFor = Exception.class)
    public void updateScoped(Room room, String role, Integer userId) {
        Room dbRoom = selectById(room.getId());
        ensureRoomAccess(dbRoom, role, userId);
        if (RoleEnum.CINEMA.name().equals(role)) {
            room.setCinemaId(userId);
        }
        validateSeatLayout(room);
        Integer cinemaId = room.getCinemaId() != null ? room.getCinemaId() : dbRoom.getCinemaId();
        if (cinemaId != null) {
            // 与 addScoped 同口径：指向不存在的影院一律拒绝，而不是写入悬空的 cinema_id
            // 并沿用旧影院名（后者会让影厅在列表里挂在一个查不到的影院下）。
            String title = cinemaTitleOf(cinemaId);
            if (title == null) {
                throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "所属影院不存在，无法确定影厅归属");
            }
            room.setTitle(title);
        }
        update(room);
    }

    /** 删除影厅：校验归属，且被排片或订单引用时禁止物理删除 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteScoped(Integer id, String role, Integer userId) {
        Room room = selectById(id);
        ensureRoomAccess(room, role, userId);
        ensureNoReferences(id);
        delete(id);
    }

    /** 批量删除影厅：逐条校验归属与引用后再删 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchScoped(List<Integer> ids, String role, Integer userId) {
        for (Integer id : ids) {
            ensureRoomAccess(selectById(id), role, userId);
            ensureNoReferences(id);
        }
        deleteBatch(ids);
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

    /** 影厅被排片或订单引用时禁止物理删除 */
    private void ensureNoReferences(Integer roomId) {
        int records = recordService.countByRoomId(roomId);
        int orders = orderedService.countByRoomId(roomId);
        if (records > 0 || orders > 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "该影厅已有 " + records + " 个排片、" + orders + " 笔订单，无法删除");
        }
    }

    /** 影厅归属校验：管理员放行，影院仅可操作本影院影厅 */
    private void ensureRoomAccess(Room room, String role, Integer userId) {
        if (room == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "影厅不存在");
        }
        if (RoleEnum.ADMIN.name().equals(role)) {
            return;
        }
        if (RoleEnum.CINEMA.name().equals(role)
                && userId != null && userId.equals(room.getCinemaId())) {
            return;
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "无权操作该影厅");
    }

    private void requireAdminOrCinema(String role) {
        if (!RoleEnum.ADMIN.name().equals(role) && !RoleEnum.CINEMA.name().equals(role)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }
}
