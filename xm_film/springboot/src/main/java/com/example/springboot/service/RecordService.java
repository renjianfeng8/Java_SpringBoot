package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RecordStatus;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.Film;
import com.example.springboot.entity.Record;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.mapper.RoomMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;

/**
 * 放映场次的增删改查，含排期时间的多种写法兼容解析、片长兜底与票价规范化
 * （票价以两位小数的 BigDecimal 落库）。
 */
@Service
@Transactional(readOnly = true)
public class RecordService extends BaseService<Record> {

    /** 影片未配置片长时的兜底时长（分钟）。排片冲突检测与取票码有效期同用此口径 */
    public static final int DEFAULT_DURATION_MINUTES = 120;

    /** 兼容前端 datetime 选择器（分钟精度）与数据库返回（秒精度、空格分隔）的多种写法 */
    private static final DateTimeFormatter START_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .optionalStart().appendPattern(" HH:mm").optionalStart().appendPattern(":ss").optionalEnd().optionalEnd()
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    private static final DateTimeFormatter STORE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RecordMapper recordMapper;

    private final FilmMapper filmMapper;

    /** 影厅归属校验直接查 Mapper，避免与 RoomService 形成互相注入的环 */
    private final RoomMapper roomMapper;

    private final OrderedService orderedService;

    public RecordService(RecordMapper recordMapper, FilmMapper filmMapper,
                         RoomMapper roomMapper, OrderedService orderedService) {
        this.recordMapper = recordMapper;
        this.filmMapper = filmMapper;
        this.roomMapper = roomMapper;
        this.orderedService = orderedService;
    }

    @Override
    protected BaseMapper<Record> mapper() {
        return recordMapper;
    }

    /**
     * 场次可购票判定 —— 全场唯一的权威规则。
     * 展示状态（未开始/放映中/已结束）由 start 派生，status 只作为人工停售开关。
     */
    public static boolean isPurchasable(Record recordItem) {
        if (recordItem == null || RecordStatus.STOPPED.equals(recordItem.getStatus())) {
            return false;
        }
        LocalDateTime start = readStart(recordItem.getStart());
        return start != null && start.isAfter(LocalDateTime.now(ZoneId.systemDefault()));
    }

    public static LocalDateTime parseStart(String raw) {
        LocalDateTime start = readStart(raw);
        if (start == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "放映时间格式错误或未填写");
        }
        return start;
    }

    public static String normalizeStatus(String status) {
        return RecordStatus.STOPPED.equals(status) ? RecordStatus.STOPPED : RecordStatus.NORMAL;
    }

    /**
     * 宽容解析数据库中的时间字符串（兼容 "yyyy-MM-dd"、"yyyy-MM-dd HH:mm"、"yyyy-MM-dd HH:mm:ss" 与 ISO 的 T 分隔）。
     * 解析失败返回 null，由调用方决定是拒绝还是降级；订单退票窗口判断复用此方法，避免第二套解析实现。
     */
    public static LocalDateTime readStart(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim().replace('T', ' '), START_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 校验排片并回填派生字段（影片名称、规范化时间与票价）。
     * 校验不通过直接抛出业务异常，避免非法场次进入前台。
     *
     * @param previousStart 编辑前的放映时间；与提交值相同时说明未改时间，
     *                      此时不强制"必须晚于当前"，以便存量过期场次仍可停售
     */
    public void validateSchedule(Record recordItem, String previousStart) {
        if (recordItem.getRoomId() == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择影厅");
        }
        if (recordItem.getFilmId() == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择影片");
        }
        Film film = filmMapper.selectById(recordItem.getFilmId());
        if (film == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "影片不存在");
        }

        LocalDateTime start = parseStart(recordItem.getStart());
        // 按解析后的时刻比较，而不是原始字符串：提交值可能只到分钟（前端选择器口径），
        // 与库里的秒精度不逐字节相等，字符串比较会把"未改时间"误判成改动，
        // 从而拒绝把一场已开演的排片改为停售。
        LocalDateTime previous = readStart(previousStart);
        if ((previous == null || !previous.equals(start))
                && !start.isAfter(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "放映时间必须晚于当前时间");
        }

        BigDecimal price = parsePrice(recordItem.getPrice());
        if (price.signum() <= 0) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "票价必须大于 0");
        }

        int duration = film.getTime() != null && film.getTime() > 0
                ? film.getTime()
                : DEFAULT_DURATION_MINUTES;
        String storedStart = start.format(STORE_FORMATTER);
        int overlap = recordMapper.countRoomOverlap(
                recordItem.getRoomId(),
                recordItem.getId(),
                storedStart,
                start.plusMinutes(duration).format(STORE_FORMATTER));
        if (overlap > 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "该影厅在此时间段已有排片");
        }

        recordItem.setTitle(film.getTitle());
        recordItem.setStart(storedStart);
        recordItem.setPrice(price.toPlainString());
    }

    public int countByFilmId(Integer filmId) {
        return recordMapper.countByFilmId(filmId);
    }

    public int countByCinemaId(Integer cinemaId) {
        return recordMapper.countByCinemaId(cinemaId);
    }

    public int countByRoomId(Integer roomId) {
        return recordMapper.countByRoomId(roomId);
    }

    /** 列表/分页范围：影院角色只能看本影院排片 */
    public void applyCinemaScope(Record recordItem, String role, Integer userId) {
        if (RoleEnum.CINEMA.name().equals(role)) {
            recordItem.setCinemaId(userId);
        }
    }

    /** 详情：仅做存在性校验（与原控制器口径一致，不在此追加归属限制） */
    public Record selectByIdScoped(Integer id, String role, Integer userId) {
        Record recordItem = selectById(id);
        if (recordItem == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "排片不存在");
        }
        return recordItem;
    }

    /** 新增排片：影院角色归属本影院，状态归一化，校验影厅归属与排期后落库 */
    @Transactional(rollbackFor = Exception.class)
    public void addScoped(Record recordItem, String role, Integer userId) {
        if (RoleEnum.CINEMA.name().equals(role)) {
            recordItem.setCinemaId(userId);
        }
        requireAdminOrCinema(role);
        // 管理员不代入影院，影院归属必须由请求给出：record.cinema_id 非空，
        // 缺省会在落库时撞 NOT NULL 报 500（ensureRoomBelongsToCinema 在 null 时直接放行）。
        if (recordItem.getCinemaId() == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择所属影院");
        }
        recordItem.setStatus(recordItem.getStatus() == null
                ? RecordStatus.NORMAL
                : normalizeStatus(recordItem.getStatus()));
        ensureRoomBelongsToCinema(recordItem);
        validateSchedule(recordItem, null);
        add(recordItem);
    }

    /** 编辑排片：先校验归属，再按局部更新口径补全缺省字段，最后校验影厅归属与排期 */
    @Transactional(rollbackFor = Exception.class)
    public void updateScoped(Record recordItem, String role, Integer userId) {
        Record dbRecord = selectById(recordItem.getId());
        ensureRecordAccess(dbRecord, role, userId);
        if (RoleEnum.CINEMA.name().equals(role)) {
            recordItem.setCinemaId(userId);
        }
        // 局部更新：未提交的字段沿用库内值，避免"仅停售"这类操作被整表校验挡住
        if (recordItem.getRoomId() == null) {
            recordItem.setRoomId(dbRecord.getRoomId());
        }
        if (recordItem.getFilmId() == null) {
            recordItem.setFilmId(dbRecord.getFilmId());
        }
        if (recordItem.getStart() == null) {
            recordItem.setStart(dbRecord.getStart());
        }
        if (recordItem.getPrice() == null) {
            recordItem.setPrice(dbRecord.getPrice());
        }
        recordItem.setStatus(recordItem.getStatus() == null
                ? dbRecord.getStatus()
                : normalizeStatus(recordItem.getStatus()));
        ensureRoomBelongsToCinema(recordItem);
        validateSchedule(recordItem, dbRecord.getStart());
        update(recordItem);
    }

    /** 删除排片：校验归属，且已产生订单的场次禁止物理删除 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteScoped(Integer id, String role, Integer userId) {
        Record recordItem = selectById(id);
        ensureRecordAccess(recordItem, role, userId);
        ensureNoOrders(id);
        delete(id);
    }

    /** 批量删除排片：逐条校验归属与订单引用后再删 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchScoped(List<Integer> ids, String role, Integer userId) {
        if (ids == null || ids.isEmpty()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择排片");
        }
        for (Integer id : ids) {
            ensureRecordAccess(selectById(id), role, userId);
            ensureNoOrders(id);
        }
        deleteBatch(ids);
    }

    /** 订单引用排片，物理删除会带走交易凭证 —— 已产生订单的场次只能停售 */
    private void ensureNoOrders(Integer recordId) {
        int count = orderedService.countByRecordId(recordId);
        if (count > 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "该场次已有 " + count + " 笔订单，无法删除；如需下架请将放映状态改为「停售」");
        }
    }

    /** 排片归属校验：管理员放行，影院仅可操作本影院排片 */
    private void ensureRecordAccess(Record recordItem, String role, Integer userId) {
        if (recordItem == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "排片不存在");
        }
        if (RoleEnum.ADMIN.name().equals(role)) {
            return;
        }
        if (RoleEnum.CINEMA.name().equals(role)
                && userId != null && userId.equals(recordItem.getCinemaId())) {
            return;
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "无权操作该排片");
    }

    /** 影厅必须存在且归属于排片所在影院 */
    private void ensureRoomBelongsToCinema(Record recordItem) {
        if (recordItem.getRoomId() == null || recordItem.getCinemaId() == null) {
            return;
        }
        Room room = roomMapper.selectById(recordItem.getRoomId());
        if (room == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "影厅不存在");
        }
        if (!recordItem.getCinemaId().equals(room.getCinemaId())) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "影厅不属于当前影院");
        }
    }

    private void requireAdminOrCinema(String role) {
        if (!RoleEnum.ADMIN.name().equals(role) && !RoleEnum.CINEMA.name().equals(role)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }

    private static BigDecimal parsePrice(String price) {
        if (price == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "票价格式错误");
        }
        try {
            return new BigDecimal(price).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "票价格式错误", e);
        }
    }
}
