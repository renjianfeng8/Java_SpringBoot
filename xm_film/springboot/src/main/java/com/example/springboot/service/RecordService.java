package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RecordStatus;
import com.example.springboot.entity.Film;
import com.example.springboot.entity.Record;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.RecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

@Service
@Transactional(readOnly = true)
public class RecordService extends BaseService<Record> {

    /** 影片未配置片长时用于冲突检测的兜底时长（分钟） */
    private static final int DEFAULT_DURATION_MINUTES = 120;

    /** 兼容前端 datetime 选择器（分钟精度）与数据库返回（秒精度、空格分隔）的多种写法 */
    private static final DateTimeFormatter START_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .optionalStart().appendPattern(" HH:mm").optionalStart().appendPattern(":ss").optionalEnd().optionalEnd()
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    private static final DateTimeFormatter STORE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private RecordMapper recordMapper;

    @Resource
    private FilmMapper filmMapper;

    @Override
    protected BaseMapper<Record> mapper() {
        return recordMapper;
    }

    /**
     * 场次可购票判定 —— 全场唯一的权威规则。
     * 展示状态（未开始/放映中/已结束）由 start 派生，status 只作为人工停售开关。
     */
    public static boolean isPurchasable(Record record) {
        if (record == null || RecordStatus.STOPPED.equals(record.getStatus())) {
            return false;
        }
        LocalDateTime start = readStart(record.getStart());
        return start != null && start.isAfter(LocalDateTime.now());
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

    private static LocalDateTime readStart(String raw) {
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
    public void validateSchedule(Record record, String previousStart) {
        if (record.getRoomId() == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择影厅");
        }
        if (record.getFilmId() == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择影片");
        }
        Film film = filmMapper.selectById(record.getFilmId());
        if (film == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "影片不存在");
        }

        LocalDateTime start = parseStart(record.getStart());
        if (!record.getStart().equals(previousStart) && !start.isAfter(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "放映时间必须晚于当前时间");
        }

        BigDecimal price = parsePrice(record.getPrice());
        if (price.signum() <= 0) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "票价必须大于 0");
        }

        int duration = film.getTime() != null && film.getTime() > 0
                ? film.getTime()
                : DEFAULT_DURATION_MINUTES;
        String storedStart = start.format(STORE_FORMATTER);
        int overlap = recordMapper.countRoomOverlap(
                record.getRoomId(),
                record.getId(),
                storedStart,
                start.plusMinutes(duration).format(STORE_FORMATTER));
        if (overlap > 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "该影厅在此时间段已有排片");
        }

        record.setTitle(film.getTitle());
        record.setStart(storedStart);
        record.setPrice(price.toPlainString());
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

    private static BigDecimal parsePrice(String price) {
        try {
            return new BigDecimal(price).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "票价格式错误");
        }
    }
}
