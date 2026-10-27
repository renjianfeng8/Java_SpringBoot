package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RecordStatus;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.Film;
import com.example.springboot.entity.Record;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.mapper.RoomMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 排片服务此前只被锁测试间接触及。这里补齐它自己的纯函数与编排规则：
 * 多种时间写法、可售判定、排期校验（锁行 → 查重叠的顺序）、局部更新与归属。
 */
@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

    private static final DateTimeFormatter STORE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mock
    private RecordMapper recordMapper;
    @Mock
    private FilmMapper filmMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private OrderedService orderedService;

    @InjectMocks
    private RecordService recordService;

    @Test
    void readStartAcceptsMinuteSecondAndIsoFormats() {
        assertThat(RecordService.readStart("2026-10-27"))
                .isEqualTo(LocalDateTime.of(2026, 10, 27, 0, 0));
        assertThat(RecordService.readStart("2026-10-27 10:30"))
                .isEqualTo(LocalDateTime.of(2026, 10, 27, 10, 30));
        assertThat(RecordService.readStart("2026-10-27 10:30:45"))
                .isEqualTo(LocalDateTime.of(2026, 10, 27, 10, 30, 45));
        assertThat(RecordService.readStart("2026-10-27T10:30:45"))
                .isEqualTo(LocalDateTime.of(2026, 10, 27, 10, 30, 45));
        assertThat(RecordService.readStart("not-a-time")).isNull();
        assertThat(RecordService.readStart(" ")).isNull();
    }

    @Test
    void parseStartRejectsInvalidValue() {
        assertThatThrownBy(() -> RecordService.parseStart("2026/10/27"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PARAM_INVALID.code()));
    }

    @Test
    void statusIsNormalizedToTheTwoKnownValues() {
        assertThat(RecordService.normalizeStatus(RecordStatus.STOPPED)).isEqualTo(RecordStatus.STOPPED);
        assertThat(RecordService.normalizeStatus("随便写的")).isEqualTo(RecordStatus.NORMAL);
        assertThat(RecordService.normalizeStatus(null)).isEqualTo(RecordStatus.NORMAL);
    }

    @Test
    void onlyFutureNonStoppedRecordIsPurchasable() {
        Record future = record(futureStart());
        assertThat(RecordService.isPurchasable(future)).isTrue();

        Record stopped = record(futureStart());
        stopped.setStatus(RecordStatus.STOPPED);
        assertThat(RecordService.isPurchasable(stopped)).isFalse();

        Record past = record(LocalDateTime.now().minusHours(1).format(STORE));
        assertThat(RecordService.isPurchasable(past)).isFalse();

        Record badTime = record("bad");
        assertThat(RecordService.isPurchasable(badTime)).isFalse();
        assertThat(RecordService.isPurchasable(null)).isFalse();
    }

    @Test
    void validateScheduleRequiresRoomAndFilm() {
        assertThatThrownBy(() -> recordService.validateSchedule(record(futureStart()), null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("请选择影厅");

        Record noFilm = record(futureStart());
        noFilm.setRoomId(1);
        assertThatThrownBy(() -> recordService.validateSchedule(noFilm, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("请选择影片");

        Record missingFilm = record(futureStart());
        missingFilm.setRoomId(1);
        missingFilm.setFilmId(3);
        when(filmMapper.selectById(3)).thenReturn(null);
        assertThatThrownBy(() -> recordService.validateSchedule(missingFilm, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("影片不存在");
    }

    @Test
    void validateScheduleRejectsPastStartAndNonPositivePrice() {
        Film film = film(3, 120);
        when(filmMapper.selectById(3)).thenReturn(film);

        Record past = record(LocalDateTime.now().minusHours(1).format(STORE));
        past.setRoomId(1);
        past.setFilmId(3);
        assertThatThrownBy(() -> recordService.validateSchedule(past, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("必须晚于当前时间");

        Record zeroPrice = record(futureStart());
        zeroPrice.setRoomId(1);
        zeroPrice.setFilmId(3);
        zeroPrice.setPrice("0");
        assertThatThrownBy(() -> recordService.validateSchedule(zeroPrice, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("票价必须大于 0");
    }

    /** 先锁影厅行、再查重叠：顺序反了并发就会落两场（BL-01） */
    @Test
    void validateScheduleLocksRoomBeforeCheckingOverlap() {
        Film film = film(3, 120);
        when(filmMapper.selectById(3)).thenReturn(film);
        when(recordMapper.countRoomOverlap(eq(1), isNull(), anyString(), anyString())).thenReturn(0);

        Record input = record(futureStart());
        input.setId(null);
        input.setRoomId(1);
        input.setFilmId(3);
        input.setPrice("45.5");

        recordService.validateSchedule(input, null);

        InOrder order = inOrder(roomMapper, recordMapper);
        order.verify(roomMapper).selectByIdForUpdate(1);
        order.verify(recordMapper).countRoomOverlap(eq(1), isNull(), anyString(), anyString());
        assertThat(input.getTitle()).isEqualTo("测试影片");
        assertThat(input.getPrice()).isEqualTo("45.50");
        assertThat(input.getStart()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void overlapIsBusinessConflict() {
        Film film = film(3, 120);
        when(filmMapper.selectById(3)).thenReturn(film);
        when(recordMapper.countRoomOverlap(eq(1), isNull(), anyString(), anyString())).thenReturn(2);

        Record input = record(futureStart());
        input.setId(null);
        input.setRoomId(1);
        input.setFilmId(3);
        input.setPrice("45.5");

        assertThatThrownBy(() -> recordService.validateSchedule(input, null))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.BUSINESS_CONFLICT.code()));
    }

    @Test
    void updateFillsMissingFieldsAndAllowsUnchangedPastStart() {
        Record db = record("2020-01-01 10:00:00");
        db.setId(5);
        db.setCinemaId(7);
        db.setRoomId(1);
        db.setFilmId(3);
        db.setPrice("40.00");
        db.setStatus(RecordStatus.NORMAL);
        when(recordMapper.selectById(5)).thenReturn(db);

        Film film = film(3, 120);
        when(filmMapper.selectById(3)).thenReturn(film);
        when(recordMapper.countRoomOverlap(eq(1), eq(5), anyString(), anyString())).thenReturn(0);

        Record input = new Record();
        input.setId(5);
        input.setStatus(RecordStatus.STOPPED);

        recordService.updateScoped(input, RoleEnum.ADMIN.name(), 1);

        assertThat(input.getRoomId()).isEqualTo(1);
        assertThat(input.getFilmId()).isEqualTo(3);
        assertThat(input.getStart()).isEqualTo(db.getStart());
        assertThat(input.getPrice()).isEqualTo("40.00");
        assertThat(input.getStatus()).isEqualTo(RecordStatus.STOPPED);
        verify(recordMapper).updateById(input);
    }

    /** 影院账号改排片：cinemaId 被 token 覆盖，不能把排片挂到别家影院 */
    @Test
    void cinemaUpdateForcesOwnCinema() {
        Record db = record(futureStart());
        db.setId(5);
        db.setCinemaId(7);
        db.setRoomId(1);
        db.setFilmId(3);
        db.setPrice("40.00");
        when(recordMapper.selectById(5)).thenReturn(db);

        Film film = film(3, 120);
        when(filmMapper.selectById(3)).thenReturn(film);
        when(roomMapper.selectById(1)).thenReturn(room(1, 7));
        when(recordMapper.countRoomOverlap(eq(1), eq(5), anyString(), anyString())).thenReturn(0);

        Record input = new Record();
        input.setId(5);
        input.setCinemaId(999);

        recordService.updateScoped(input, RoleEnum.CINEMA.name(), 7);

        assertThat(input.getCinemaId()).isEqualTo(7);
        verify(recordMapper).updateById(input);
    }

    @Test
    void updateRejectsOtherCinemasRecord() {
        Record db = record(futureStart());
        db.setId(5);
        db.setCinemaId(7);
        when(recordMapper.selectById(5)).thenReturn(db);

        assertThatThrownBy(() -> recordService.updateScoped(db, RoleEnum.CINEMA.name(), 8))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.FORBIDDEN.code()));
    }

    @Test
    void deleteLocksRowBeforeCountingOrders() {
        when(recordMapper.selectByIdForUpdate(5)).thenReturn(record(futureStart()));
        when(orderedService.countByRecordId(5)).thenReturn(0);

        recordService.deleteScoped(5, RoleEnum.ADMIN.name(), 1);

        verify(recordMapper).selectByIdForUpdate(5);
        verify(recordMapper, never()).selectById(5);
        verify(recordMapper).deleteById(5);
    }

    @Test
    void deleteIsRejectedWhenOrdersExist() {
        when(recordMapper.selectByIdForUpdate(5)).thenReturn(record(futureStart()));
        when(orderedService.countByRecordId(5)).thenReturn(2);

        assertThatThrownBy(() -> recordService.deleteScoped(5, RoleEnum.ADMIN.name(), 1))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("已有 2 笔订单");
        verify(recordMapper, never()).deleteById(5);
    }

    @Test
    void batchDeleteRequiresNonEmptyIds() {
        assertThatThrownBy(() -> recordService.deleteBatchScoped(List.of(), RoleEnum.ADMIN.name(), 1))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PARAM_INVALID.code()));
    }

    @Test
    void batchDeleteAggregatesBlockedRows() {
        Record first = record(futureStart());
        first.setId(1);
        Record second = record(futureStart());
        second.setId(2);
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(first);
        when(recordMapper.selectByIdForUpdate(2)).thenReturn(second);
        when(orderedService.countByRecordId(1)).thenReturn(0);
        when(orderedService.countByRecordId(2)).thenReturn(1);

        assertThatThrownBy(() -> recordService.deleteBatchScoped(List.of(1, 2), RoleEnum.ADMIN.name(), 1))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("排片 #2")
                .hasMessageContaining("整批未删除");
        verify(recordMapper, never()).deleteBatch(any());
    }

    @Test
    void applyCinemaScopeOnlyOverridesCinemaRole() {
        Record target = record(futureStart());
        target.setCinemaId(99);

        recordService.applyCinemaScope(target, RoleEnum.CINEMA.name(), 7);
        assertThat(target.getCinemaId()).isEqualTo(7);

        Record adminTarget = record(futureStart());
        adminTarget.setCinemaId(99);
        recordService.applyCinemaScope(adminTarget, RoleEnum.ADMIN.name(), 1);
        assertThat(adminTarget.getCinemaId()).isEqualTo(99);
    }

    @Test
    void selectByIdScopedMissingRecordIsNotFound() {
        when(recordMapper.selectById(5)).thenReturn(null);

        assertThatThrownBy(() -> recordService.selectByIdScoped(5))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.NOT_FOUND.code()));
    }

    private static String futureStart() {
        return LocalDateTime.now(ZoneId.systemDefault()).plusDays(1).withNano(0).format(STORE);
    }

    private static Record record(String start) {
        Record record = new Record();
        record.setId(1);
        record.setTitle("测试影片");
        record.setStart(start);
        record.setStatus(RecordStatus.NORMAL);
        return record;
    }

    private static Film film(int id, int minutes) {
        Film film = new Film();
        film.setId(id);
        film.setTitle("测试影片");
        film.setTime(minutes);
        return film;
    }

    private static com.example.springboot.entity.Room room(int id, int cinemaId) {
        com.example.springboot.entity.Room room = new com.example.springboot.entity.Room();
        room.setId(id);
        room.setCinemaId(cinemaId);
        return room;
    }
}
