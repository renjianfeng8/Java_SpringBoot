package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.RoomMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 影厅服务此前零直接测试。它承载三类容易被绕过的规则：
 * 影院归属（只能动本影院）、座位规模（1~50）、以及被排片/订单引用时禁止物理删除。
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomMapper roomMapper;
    @Mock
    private RecordService recordService;
    @Mock
    private OrderedService orderedService;
    @Mock
    private CinemaService cinemaService;

    @InjectMocks
    private RoomService roomService;

    @Test
    void adminCanReadAnyRoom() {
        Room room = room(1, 7);
        when(roomMapper.selectById(1)).thenReturn(room);

        assertThat(roomService.selectByIdScoped(1, RoleEnum.ADMIN.name(), 999)).isSameAs(room);
    }

    @Test
    void cinemaCanReadOwnRoomOnly() {
        Room room = room(1, 7);
        when(roomMapper.selectById(1)).thenReturn(room);

        assertThat(roomService.selectByIdScoped(1, RoleEnum.CINEMA.name(), 7)).isSameAs(room);
        assertThatThrownBy(() -> roomService.selectByIdScoped(1, RoleEnum.CINEMA.name(), 8))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.FORBIDDEN.code()));
    }

    @Test
    void missingRoomIsNotFound() {
        when(roomMapper.selectById(1)).thenReturn(null);

        assertThatThrownBy(() -> roomService.selectByIdScoped(1, RoleEnum.ADMIN.name(), 1))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.NOT_FOUND.code()));
    }

    @Test
    void userCannotCreateRoom() {
        assertThatThrownBy(() -> roomService.addScoped(room(null, 7), RoleEnum.USER.name(), 11))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.FORBIDDEN.code()));

        verify(roomMapper, never()).insert(any());
    }

    @Test
    void addRequiresCinema() {
        assertThatThrownBy(() -> roomService.addScoped(room(null, null), RoleEnum.ADMIN.name(), 1))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PARAM_INVALID.code()));
    }

    @Test
    void seatDimensionsMustStayWithinOneToFifty() {
        Room zero = room(null, 7);
        zero.setSeatRows(0);
        assertThatThrownBy(() -> roomService.addScoped(zero, RoleEnum.ADMIN.name(), 1))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("座位行数");

        Room tooLarge = room(null, 7);
        tooLarge.setSeatCols(51);
        assertThatThrownBy(() -> roomService.addScoped(tooLarge, RoleEnum.ADMIN.name(), 1))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("座位列数");
    }

    @Test
    void addRejectsDanglingCinema() {
        when(cinemaService.selectByIdForUpdate(7)).thenReturn(null);

        assertThatThrownBy(() -> roomService.addScoped(room(null, 7), RoleEnum.ADMIN.name(), 1))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.BUSINESS_CONFLICT.code()));

        verify(roomMapper, never()).insert(any());
    }

    /** 影院名不接受前端手填，必须从影院记录派生 */
    @Test
    void addDerivesCinemaNameAndDelegatesInsert() {
        Cinema cinema = new Cinema();
        cinema.setId(7);
        cinema.setName("测试影院");
        when(cinemaService.selectByIdForUpdate(7)).thenReturn(cinema);
        when(cinemaService.selectById(7)).thenReturn(cinema);

        Room input = room(null, 7);
        input.setTitle("前端乱填的影院名");
        input.setName("1号厅");

        roomService.addScoped(input, RoleEnum.ADMIN.name(), 1);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper).insert(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("测试影院");
        assertThat(captor.getValue().getName()).isEqualTo("1号厅");
    }

    @Test
    void updateMissingRoomIsNotFound() {
        Room input = room(1, 7);
        when(roomMapper.selectById(1)).thenReturn(null);

        assertThatThrownBy(() -> roomService.updateScoped(input, RoleEnum.ADMIN.name(), 1))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.NOT_FOUND.code()));
    }

    /** 影院账号改影厅时，归属被 token 覆盖，不能把影厅搬到别家影院 */
    @Test
    void cinemaUpdateForcesOwnCinemaAndReDerivesTitle() {
        Room db = room(1, 7);
        Cinema cinema = new Cinema();
        cinema.setId(7);
        cinema.setName("测试影院");
        when(roomMapper.selectById(1)).thenReturn(db);
        when(cinemaService.selectById(7)).thenReturn(cinema);

        Room input = room(1, 999);
        input.setSeatRows(10);
        input.setSeatCols(10);
        input.setCinemaId(999);

        roomService.updateScoped(input, RoleEnum.CINEMA.name(), 7);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomMapper).updateById(captor.capture());
        assertThat(captor.getValue().getCinemaId()).isEqualTo(7);
        assertThat(captor.getValue().getTitle()).isEqualTo("测试影院");
    }

    @Test
    void updateRejectsDanglingCinema() {
        Room db = room(1, 7);
        when(roomMapper.selectById(1)).thenReturn(db);
        when(cinemaService.selectById(7)).thenReturn(null);

        Room input = room(1, 7);
        assertThatThrownBy(() -> roomService.updateScoped(input, RoleEnum.ADMIN.name(), 1))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.BUSINESS_CONFLICT.code()));

        verify(roomMapper, never()).updateById(any());
    }

    @Test
    void deleteIsBlockedByScheduleOrOrders() {
        when(roomMapper.selectById(1)).thenReturn(room(1, 7));
        when(recordService.countByRoomId(1)).thenReturn(1);
        when(orderedService.countByRoomId(1)).thenReturn(0);

        assertThatThrownBy(() -> roomService.deleteScoped(1, RoleEnum.ADMIN.name(), 1))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.BUSINESS_CONFLICT.code()));

        verify(roomMapper, never()).deleteById(1);
    }

    @Test
    void deleteProceedsWhenUnreferenced() {
        when(roomMapper.selectById(1)).thenReturn(room(1, 7));
        when(recordService.countByRoomId(1)).thenReturn(0);
        when(orderedService.countByRoomId(1)).thenReturn(0);

        roomService.deleteScoped(1, RoleEnum.ADMIN.name(), 1);

        verify(roomMapper).deleteById(1);
    }

    @Test
    void batchDeleteRequiresNonEmptyIds() {
        assertThatThrownBy(() -> roomService.deleteBatchScoped(List.of(), RoleEnum.ADMIN.name(), 1))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PARAM_INVALID.code()));
    }

    /** 批量删除“全有或全无”：一条被引用就整批不删，并点名到具体影厅 */
    @Test
    void batchDeleteAggregatesBlockedRowsAndSkipsDelete() {
        when(roomMapper.selectById(1)).thenReturn(room(1, 7));
        when(roomMapper.selectById(2)).thenReturn(room(2, 7));
        when(recordService.countByRoomId(1)).thenReturn(0);
        when(orderedService.countByRoomId(1)).thenReturn(0);
        when(recordService.countByRoomId(2)).thenReturn(3);
        when(orderedService.countByRoomId(2)).thenReturn(0);

        assertThatThrownBy(() -> roomService.deleteBatchScoped(List.of(1, 2), RoleEnum.ADMIN.name(), 1))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("影厅 #2")
                .hasMessageContaining("整批未删除");

        verify(roomMapper, never()).deleteBatch(any());
    }

    @Test
    void batchDeleteProceedsWhenAllClear() {
        when(roomMapper.selectById(1)).thenReturn(room(1, 7));
        when(recordService.countByRoomId(1)).thenReturn(0);
        when(orderedService.countByRoomId(1)).thenReturn(0);

        roomService.deleteBatchScoped(List.of(1), RoleEnum.ADMIN.name(), 1);

        verify(roomMapper).deleteBatch(List.of(1));
    }

    private static Room room(Integer id, Integer cinemaId) {
        Room room = new Room();
        room.setId(id);
        room.setCinemaId(cinemaId);
        room.setName("1号厅");
        room.setSeatRows(10);
        room.setSeatCols(10);
        return room;
    }
}
