package com.example.springboot;

import com.example.springboot.entity.Ordered;
import com.example.springboot.entity.Record;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.service.OrderedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = OrderedService.class)
class OrderedServiceTest {

    @Autowired
    OrderedService orderedService;

    @MockBean
    OrderedMapper orderedMapper;

    @MockBean
    RecordMapper recordMapper;

    @MockBean
    FilmMapper filmMapper;

    // ========== 反向用例 ==========

    @Test
    void userCannotCancelAnotherUsersOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待支付");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.cancelOrder(1, "USER", 200))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void cancelledOrderCannotBePickedUp() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setCinemaId(10);
        ordered.setStatus("已取消");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.pickupOrder(1, "CINEMA", 10))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("状态");
    }

    @Test
    void userCannotPickupOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待取票");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.pickupOrder(1, "USER", 100))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void userCannotReadAnotherUsersOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        when(orderedMapper.selectById(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.selectByIdScoped(1, "USER", 200))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void genericOrderUpdateIsRejected() {
        Ordered update = new Ordered();
        update.setId(1);
        update.setSeat("1排1座");

        assertThatThrownBy(() -> orderedService.updateScoped(update, "ADMIN", 999))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("显式");
    }

    @Test
    void createOrderLocksRecordBeforeCheckingSeats() {
        Record record = new Record();
        record.setId(1);
        record.setFilmId(24);
        record.setCinemaId(10);
        record.setRoomId(7);
        record.setPrice("45.00");
        record.setStart("2026-06-12 20:00");
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(record);
        when(orderedMapper.countSeatInUse(1, "1排1座")).thenReturn(0);

        Ordered ordered = new Ordered();
        ordered.setRecordId(1);
        ordered.setSeat("1排1座");

        orderedService.createOrder(ordered, "USER", 8);

        verify(recordMapper).selectByIdForUpdate(1);
        verify(orderedMapper).insert(ordered);
    }

    @Test
    void cancelOrderLocksOrderBeforeChangingStatus() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待支付");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        orderedService.cancelOrder(1, "USER", 100);

        verify(orderedMapper).selectByIdForUpdate(1);
    }

    // ========== 正向通路 ==========

    @Test
    void userCancelsOwnOrderSuccessfully() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待支付");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        orderedService.cancelOrder(1, "USER", 100);

        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId() && "已取消".equals(u.getStatus())
        ));
    }

    @Test
    void cinemaPickupOrderSuccessfully() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setCinemaId(10);
        ordered.setStatus("待取票");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        orderedService.pickupOrder(1, "CINEMA", 10);

        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId() && "已取票".equals(u.getStatus())
        ));
    }

    @Test
    void cinemaCancelsOrderSuccessfully() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setCinemaId(10);
        ordered.setStatus("待支付");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        orderedService.cancelOrder(1, "CINEMA", 10);

        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId() && "已取消".equals(u.getStatus())
        ));
    }

    @Test
    void adminCancelsOrderSuccessfully() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setCinemaId(10);
        ordered.setStatus("待支付");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        orderedService.cancelOrder(1, "ADMIN", 999);

        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId() && "已取消".equals(u.getStatus())
        ));
    }

    @Test
    void adminPickupOrderSuccessfully() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setCinemaId(10);
        ordered.setStatus("待取票");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        orderedService.pickupOrder(1, "ADMIN", 999);

        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId() && "已取票".equals(u.getStatus())
        ));
    }

    @Test
    void cinemaReadsOwnOrderSuccessfully() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setCinemaId(10);
        when(orderedMapper.selectById(1)).thenReturn(ordered);

        Ordered result = orderedService.selectByIdScoped(1, "CINEMA", 10);

        assertThat(result).isSameAs(ordered);
    }
}
