package com.example.springboot;

import com.example.springboot.common.enums.PayResult;
import com.example.springboot.entity.Ordered;
import com.example.springboot.entity.Record;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.mapper.RoomMapper;
import com.example.springboot.service.OrderedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    @MockBean
    RoomMapper roomMapper;

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
        Record recordRow = new Record();
        recordRow.setId(1);
        recordRow.setFilmId(24);
        recordRow.setCinemaId(10);
        recordRow.setRoomId(7);
        recordRow.setPrice("45.00");
        recordRow.setStart(futureStart());
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(recordRow);
        when(orderedMapper.countSeatInUse(1, "1排1座")).thenReturn(0);

        Ordered ordered = new Ordered();
        ordered.setRecordId(1);
        ordered.setSeat("1排1座");

        orderedService.createOrder(ordered, "USER", 8);

        verify(recordMapper).selectByIdForUpdate(1);
        verify(orderedMapper).insert(ordered);
    }

    @Test
    void cannotCreateOrderForStartedRecord() {
        Record recordRow = new Record();
        recordRow.setId(1);
        recordRow.setFilmId(24);
        recordRow.setPrice("45.00");
        recordRow.setStart("2020-01-01 10:00");
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(recordRow);

        Ordered ordered = new Ordered();
        ordered.setRecordId(1);
        ordered.setSeat("1排1座");

        assertThatThrownBy(() -> orderedService.createOrder(ordered, "USER", 8))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("已开场");
    }

    @Test
    void cannotCreateOrderForStoppedRecord() {
        Record recordRow = new Record();
        recordRow.setId(1);
        recordRow.setFilmId(24);
        recordRow.setPrice("45.00");
        recordRow.setStatus("停售");
        recordRow.setStart(futureStart());
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(recordRow);

        Ordered ordered = new Ordered();
        ordered.setRecordId(1);
        ordered.setSeat("1排1座");

        assertThatThrownBy(() -> orderedService.createOrder(ordered, "USER", 8))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("停售");
    }

    private static String futureStart() {
        return LocalDateTime.now().plusDays(1).withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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

    // ========== 支付状态机 + 资金凭证 ==========

    @Test
    void payOrderRecordsPaymentCredential() {
        Ordered ordered = pendingPaymentOrder();
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        PayResult result = orderedService.payOrder(1, "USER", 100);

        assertThat(result).isEqualTo(PayResult.PAID);
        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId()
                        && "待取票".equals(u.getStatus())
                        && u.getPayTime() != null
                        && Double.valueOf(90.0).equals(u.getPayAmount())
        ));
    }

    /**
     * 超时分支必须"取消落库 + 返回超时结果"。
     * 若实现改为抛异常，@Transactional(rollbackFor = Exception.class) 会把取消一并回滚，
     * 订单又回到待支付，本用例即失败。
     */
    @Test
    void payOrderAfterTimeoutCancelsOrderInsteadOfThrowing() {
        Ordered ordered = pendingPaymentOrder();
        ordered.setPendingTimeoutAt(minutesFromNow(-1));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        PayResult result = orderedService.payOrder(1, "USER", 100);

        assertThat(result).isEqualTo(PayResult.TIMEOUT_CANCELLED);
        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId() && "已取消".equals(u.getStatus())
        ));
    }

    @Test
    void cannotPayAlreadyPaidOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待取票");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.payOrder(1, "USER", 100))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("不允许支付");
    }

    // ========== 退票 ==========

    @Test
    void userRefundsOwnPaidOrderBeforeDeadline() {
        Ordered ordered = paidOrder();
        ordered.setStart(minutesFromNow(120));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        orderedService.refundOrder(1, "USER", 100);

        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId()
                        && "已退票".equals(u.getStatus())
                        && u.getRefundTime() != null
                        && Double.valueOf(90.0).equals(u.getRefundAmount())
        ));
    }

    @Test
    void cannotRefundUnpaidOrder() {
        Ordered ordered = pendingPaymentOrder();
        ordered.setStart(minutesFromNow(120));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.refundOrder(1, "USER", 100))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("仅待取票");
    }

    @Test
    void cannotRefundPickedUpOrder() {
        Ordered ordered = paidOrder();
        ordered.setStatus("已取票");
        ordered.setStart(minutesFromNow(120));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.refundOrder(1, "USER", 100))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("仅待取票");
    }

    @Test
    void cannotRefundWithinDeadlineBeforeStart() {
        Ordered ordered = paidOrder();
        ordered.setStart(minutesFromNow(30));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.refundOrder(1, "USER", 100))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("退票截止");
    }

    @Test
    void userCannotRefundAnotherUsersOrder() {
        Ordered ordered = paidOrder();
        ordered.setStart(minutesFromNow(120));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.refundOrder(1, "USER", 200))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("无权");
    }

    // ========== 影厅座位容量 ==========

    @Test
    void seatWithinRoomLayoutIsAcceptedAndOutsideIsRejected() {
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(futureRecord());
        Room room = new Room();
        room.setId(7);
        room.setSeatRows(6);
        room.setSeatCols(6);
        when(roomMapper.selectById(7)).thenReturn(room);

        Ordered onEdge = new Ordered();
        onEdge.setRecordId(1);
        onEdge.setSeat("6排6座");
        orderedService.createOrder(onEdge, "USER", 8);
        verify(orderedMapper).insert(onEdge);

        Ordered outside = new Ordered();
        outside.setRecordId(1);
        outside.setSeat("7排1座");
        assertThatThrownBy(() -> orderedService.createOrder(outside, "USER", 8))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("超出影厅范围");
    }

    /** 影厅未配置座位数时退回 8×8，保证存量数据仍可下单 */
    @Test
    void seatFallsBackToDefaultEightByEightWhenRoomHasNoLayout() {
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(futureRecord());
        Room room = new Room();
        room.setId(7);
        when(roomMapper.selectById(7)).thenReturn(room);

        Ordered ordered = new Ordered();
        ordered.setRecordId(1);
        ordered.setSeat("8排8座");

        orderedService.createOrder(ordered, "USER", 8);

        verify(orderedMapper).insert(ordered);
    }

    @Test
    void moreThanSixSeatsInOneOrderIsRejected() {
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(futureRecord());

        Ordered ordered = new Ordered();
        ordered.setRecordId(1);
        ordered.setSeat("1排1座,1排2座,1排3座,1排4座,1排5座,1排6座,1排7座");

        assertThatThrownBy(() -> orderedService.createOrder(ordered, "USER", 8))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("最多选择 6 个座位");
    }

    private Record futureRecord() {
        Record recordRow = new Record();
        recordRow.setId(1);
        recordRow.setFilmId(24);
        recordRow.setCinemaId(10);
        recordRow.setRoomId(7);
        recordRow.setPrice("45.00");
        recordRow.setStart(futureStart());
        return recordRow;
    }

    private Ordered pendingPaymentOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待支付");
        ordered.setTotal(90.0);
        return ordered;
    }

    private Ordered paidOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待取票");
        ordered.setTotal(90.0);
        return ordered;
    }

    private static String minutesFromNow(int minutes) {
        return LocalDateTime.now().plusMinutes(minutes).withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
