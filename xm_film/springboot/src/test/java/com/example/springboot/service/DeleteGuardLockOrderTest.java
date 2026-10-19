package com.example.springboot.service;

import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.Film;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.mapper.RoomMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 守卫（架构修复 1）：影院/影片的删除守卫必须是「先锁父行 → 再数子表 → 最后删除」，
 * 且计数与删除在同一事务内（即同一方法，类上标 {@code @Transactional}）。
 *
 * <p><b>缺陷</b>：守卫原来放在 Controller，计数与 {@code delete} 分属两个事务。
 * 管理员数到「0 个影厅」之后、DELETE 之前，影院角色可以插入一个影厅，DELETE 随即撞
 * 外键 RESTRICT —— 数据不会坏，但管理员拿到的是通用提示而非「已有 1 个影厅」。
 *
 * <p><b>本测试能证明什么</b>：加锁读发生在<b>计数之前</b>（InOrder 断言），且冲突时不执行删除。
 * <b>不能证明</b>：两个真实事务并发时锁确实互斥 —— 那需要双连接测试，
 * 见 {@code audit/backend-backlog.md} §测试覆盖缺口。
 */
@ExtendWith(MockitoExtension.class)
class DeleteGuardLockOrderTest {

    @Mock
    private CinemaMapper cinemaMapper;
    @Mock
    private CinemaDirectoryMapper directoryMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private RecordMapper recordMapper;
    @Mock
    private OrderedMapper orderedMapper;
    @Mock
    private FilmMapper filmMapper;

    private CinemaService cinemaService;
    private FilmService filmService;

    @BeforeEach
    void setUp() {
        cinemaService = new CinemaService(cinemaMapper, directoryMapper);
        ReflectionTestUtils.invokeMethod(cinemaService, "setReferenceMappers",
                roomMapper, recordMapper, orderedMapper);

        filmService = new FilmService(filmMapper);
        ReflectionTestUtils.invokeMethod(filmService, "setReferenceMappers",
                recordMapper, orderedMapper);
    }

    @Test
    void cinemaDeleteLocksRowBeforeCountingReferences() {
        when(cinemaMapper.selectByIdForUpdate(7)).thenReturn(new Cinema());
        when(roomMapper.countByCinemaId(7)).thenReturn(0);
        when(recordMapper.countByCinemaId(7)).thenReturn(0);
        when(orderedMapper.countByCinemaId(7)).thenReturn(0);

        cinemaService.deleteGuarded(List.of(7));

        InOrder order = inOrder(cinemaMapper, roomMapper, recordMapper, orderedMapper);
        order.verify(cinemaMapper).selectByIdForUpdate(7);
        order.verify(roomMapper).countByCinemaId(7);
        order.verify(recordMapper).countByCinemaId(7);
        order.verify(orderedMapper).countByCinemaId(7);
        order.verify(cinemaMapper).deleteBatch(List.of(7));
    }

    @Test
    void cinemaDeleteRefusesWhenAnyReferenceExists() {
        when(cinemaMapper.selectByIdForUpdate(7)).thenReturn(new Cinema());
        when(roomMapper.countByCinemaId(7)).thenReturn(2);

        assertThatThrownBy(() -> cinemaService.deleteGuarded(List.of(7)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("2 个影厅");

        verify(cinemaMapper, never()).deleteBatch(any());
    }

    @Test
    void cinemaDeleteRejectsMissingRow() {
        when(cinemaMapper.selectByIdForUpdate(99)).thenReturn(null);

        assertThatThrownBy(() -> cinemaService.deleteGuarded(List.of(99)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("不存在");

        verifyNoInteractions(roomMapper, recordMapper, orderedMapper);
        verify(cinemaMapper, never()).deleteBatch(any());
    }

    /** 批量按 id 升序加锁：避免 [A,B] 与 [B,A] 交叉等待死锁 */
    @Test
    void cinemaBatchDeleteLocksInAscendingIdOrder() {
        when(cinemaMapper.selectByIdForUpdate(any())).thenAnswer(inv -> new Cinema());
        when(roomMapper.countByCinemaId(any())).thenReturn(0);
        when(recordMapper.countByCinemaId(any())).thenReturn(0);
        when(orderedMapper.countByCinemaId(any())).thenReturn(0);

        cinemaService.deleteGuarded(List.of(9, 3, 5));

        InOrder order = inOrder(cinemaMapper);
        order.verify(cinemaMapper).selectByIdForUpdate(3);
        order.verify(cinemaMapper).selectByIdForUpdate(5);
        order.verify(cinemaMapper).selectByIdForUpdate(9);
        verify(cinemaMapper).deleteBatch(List.of(3, 5, 9));
    }

    @Test
    void filmDeleteLocksRowBeforeCountingReferences() {
        when(filmMapper.selectByIdForUpdate(11)).thenReturn(new Film());
        when(recordMapper.countByFilmId(11)).thenReturn(0);
        when(orderedMapper.countByFilmId(11)).thenReturn(0);

        filmService.deleteGuarded(List.of(11));

        InOrder order = inOrder(filmMapper, recordMapper, orderedMapper);
        order.verify(filmMapper).selectByIdForUpdate(11);
        order.verify(recordMapper).countByFilmId(11);
        order.verify(orderedMapper).countByFilmId(11);
        order.verify(filmMapper).deleteBatch(List.of(11));
    }

    @Test
    void filmDeleteRefusesWhenReferencedByRecord() {
        when(filmMapper.selectByIdForUpdate(11)).thenReturn(new Film());
        when(recordMapper.countByFilmId(11)).thenReturn(4);

        assertThatThrownBy(() -> filmService.deleteGuarded(List.of(11)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("4 个排片")
                .hasMessageContaining("停止上映");

        verify(filmMapper, never()).deleteBatch(any());
    }
}
