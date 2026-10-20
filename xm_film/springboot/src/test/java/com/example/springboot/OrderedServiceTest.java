package com.example.springboot;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.PayResult;
import com.example.springboot.common.enums.RecordStatus;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.response.SeatOccupancyView;
import com.example.springboot.dto.response.TicketVoucherView;
import com.example.springboot.dto.response.TodayRevenueView;
import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.Film;
import com.example.springboot.entity.Ordered;
import com.example.springboot.entity.Record;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.mapper.RoomMapper;
import com.example.springboot.service.OrderedService;
import com.example.springboot.service.PayPasswordService;
import com.example.springboot.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.example.springboot.common.OrderStatusCriteria;
import com.example.springboot.common.enums.OrderStatus;

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

    @MockBean
    CinemaMapper cinemaMapper;

    @MockBean
    WalletService walletService;

    @MockBean
    PayPasswordService payPasswordService;

    /** 支付密码只在服务里被原样转交给 PayPasswordService 校验，测试里给个定值即可 */
    private static final String PAY_PASSWORD = "123456";

    // ========== 反向用例 ==========

    @Test
    void userCannotCancelAnotherUsersOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待支付");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.cancelOrder(1, RoleEnum.USER.name(), 200))
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

        assertThatThrownBy(() -> orderedService.pickupOrder(1, RoleEnum.CINEMA.name(), 10))
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

        // 用户即使操作自己的订单也走不通柜台通路；提示把他导向取票大厅的自助核销
        assertThatThrownBy(() -> orderedService.pickupOrder(1, RoleEnum.USER.name(), 100))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("影院柜台");
    }

    @Test
    void userCannotReadAnotherUsersOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        when(orderedMapper.selectById(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.selectByIdScoped(1, RoleEnum.USER.name(), 200))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void genericOrderUpdateIsRejected() {
        Ordered update = new Ordered();
        update.setId(1);
        update.setSeat("1排1座");

        assertThatThrownBy(() -> orderedService.updateScoped(update, RoleEnum.ADMIN.name(), 999))
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
        when(orderedMapper.countSeatInUse(1, "1排1座", OrderStatusCriteria.INSTANCE)).thenReturn(0);

        Ordered ordered = new Ordered();
        ordered.setRecordId(1);
        ordered.setSeat("1排1座");

        orderedService.createOrder(ordered, RoleEnum.USER.name(), 8);

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

        assertThatThrownBy(() -> orderedService.createOrder(ordered, RoleEnum.USER.name(), 8))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("已开场");
    }

    /**
     * 票价为非法值时，下单返回 {@code PARAM_INVALID}（400），而不是
     * {@code SYSTEM_ERROR}（500）。
     *
     * <p><b>这是单元级契约，不代表生产可复现</b>：{@code record.price} 是
     * {@code decimal(10,2)}，库层拒绝非法字符串（实测 SQL 直接报错），
     * 所以真实链路里 {@code Record.getPrice()} 不会拿到 "abc"。
     * 本用例守的是"解析失败时的错误码口径与录入侧一致" —— 一旦将来列类型放宽，
     * 或者有人把这条解析搬去处理用户输入，错误码不能退回 500。
     */
    @Test
    void invalidRecordPriceIsParamInvalidNotSystemError() {
        Record recordRow = new Record();
        recordRow.setId(1);
        recordRow.setFilmId(24);
        recordRow.setCinemaId(10);
        recordRow.setRoomId(7);
        recordRow.setPrice("abc");
        recordRow.setStart(futureStart());
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(recordRow);
        when(orderedMapper.countSeatInUse(1, "1排1座", OrderStatusCriteria.INSTANCE)).thenReturn(0);

        Ordered ordered = new Ordered();
        ordered.setRecordId(1);
        ordered.setSeat("1排1座");

        assertThatThrownBy(() -> orderedService.createOrder(ordered, RoleEnum.USER.name(), 8))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_INVALID.code()));
        verify(orderedMapper, never()).insert(any(Ordered.class));
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

        assertThatThrownBy(() -> orderedService.createOrder(ordered, RoleEnum.USER.name(), 8))
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

        orderedService.cancelOrder(1, RoleEnum.USER.name(), 100);

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

        orderedService.cancelOrder(1, RoleEnum.USER.name(), 100);

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

        orderedService.pickupOrder(1, RoleEnum.CINEMA.name(), 10);

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

        orderedService.cancelOrder(1, RoleEnum.CINEMA.name(), 10);

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

        orderedService.cancelOrder(1, RoleEnum.ADMIN.name(), 999);

        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId() && "已取消".equals(u.getStatus())
        ));
    }

    @Test
    void adminCannotPickupOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setCinemaId(10);
        ordered.setStatus("待取票");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        // 取票是影院柜台的物理交付动作，ADMIN 不受 cinemaId 约束、放行等于可伪造任意用户的取票
        assertThatThrownBy(() -> orderedService.pickupOrder(1, RoleEnum.ADMIN.name(), 999))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("影院柜台");

        verify(orderedMapper, never()).updateById(any());
    }

    @Test
    void cinemaReadsOwnOrderSuccessfully() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setCinemaId(10);
        when(orderedMapper.selectById(1)).thenReturn(ordered);

        Ordered result = orderedService.selectByIdScoped(1, RoleEnum.CINEMA.name(), 10);

        assertThat(result).isSameAs(ordered);
    }

    // ========== 支付状态机 + 资金凭证 ==========

    @Test
    void payOrderRecordsPaymentCredential() {
        Ordered ordered = pendingPaymentOrder();
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(purchasableRecord());

        PayResult result = orderedService.payOrder(1, RoleEnum.USER.name(), 100, PAY_PASSWORD);

        assertThat(result).isEqualTo(PayResult.PAID);
        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId()
                        && "待取票".equals(u.getStatus())
                        && u.getPayTime() != null
                        && u.getPayAmount().compareTo(new BigDecimal("90.00")) == 0
        ));
    }

    /**
     * 超时分支必须"取消落库 + 返回超时结果"。
     * 若实现改为抛异常，@Transactional(rollbackFor = Exception.class) 会把取消一并回滚，
     * 订单又回到待支付，本用例即失败。
     *
     * <p>BL-05 之后「是否超时」由数据库判定（{@code countExpiredById}，与清理任务共用
     * {@code pendingExpiredCondition}），所以这里给的是<b>库的答案</b>而不是
     * 一个 JVM 时间字符串。刻意**不**给排片行打桩：超时分支必须在复核场次之前返回，
     * 若实现顺序反了，本用例会因「场次已不存在」而失败。
     */
    @Test
    void payOrderAfterTimeoutCancelsOrderInsteadOfThrowing() {
        Ordered ordered = pendingPaymentOrder();
        ordered.setPendingTimeoutAt(minutesFromNow(-1));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);
        when(orderedMapper.countExpiredById(1, OrderStatusCriteria.INSTANCE)).thenReturn(1);

        PayResult result = orderedService.payOrder(1, RoleEnum.USER.name(), 100, PAY_PASSWORD);

        assertThat(result).isEqualTo(PayResult.TIMEOUT_CANCELLED);
        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId() && "已取消".equals(u.getStatus())
        ));
        verifyNoInteractions(walletService);
    }

    @Test
    void cannotPayAlreadyPaidOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待取票");
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.payOrder(1, RoleEnum.USER.name(), 100, PAY_PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("不允许支付");
    }

    // ========== 退票 ==========

    @Test
    void userRefundsOwnPaidOrderBeforeDeadline() {
        Ordered ordered = paidOrder();
        ordered.setStart(minutesFromNow(120));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        orderedService.refundOrder(1, RoleEnum.USER.name(), 100);

        verify(orderedMapper).updateById(argThat(u ->
                1 == u.getId()
                        && "已退票".equals(u.getStatus())
                        && u.getRefundTime() != null
                        && u.getRefundAmount().compareTo(new BigDecimal("90.00")) == 0
        ));
    }

    @Test
    void cannotRefundUnpaidOrder() {
        Ordered ordered = pendingPaymentOrder();
        ordered.setStart(minutesFromNow(120));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.refundOrder(1, RoleEnum.USER.name(), 100))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("仅待取票");
    }

    @Test
    void cannotRefundPickedUpOrder() {
        Ordered ordered = paidOrder();
        ordered.setStatus("已取票");
        ordered.setStart(minutesFromNow(120));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.refundOrder(1, RoleEnum.USER.name(), 100))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("仅待取票");
    }

    @Test
    void cannotRefundWithinDeadlineBeforeStart() {
        Ordered ordered = paidOrder();
        ordered.setStart(minutesFromNow(30));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.refundOrder(1, RoleEnum.USER.name(), 100))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("退票截止");
    }

    @Test
    void userCannotRefundAnotherUsersOrder() {
        Ordered ordered = paidOrder();
        ordered.setStart(minutesFromNow(120));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.refundOrder(1, RoleEnum.USER.name(), 200))
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
        orderedService.createOrder(onEdge, RoleEnum.USER.name(), 8);
        verify(orderedMapper).insert(onEdge);

        Ordered outside = new Ordered();
        outside.setRecordId(1);
        outside.setSeat("7排1座");
        assertThatThrownBy(() -> orderedService.createOrder(outside, RoleEnum.USER.name(), 8))
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

        orderedService.createOrder(ordered, RoleEnum.USER.name(), 8);

        verify(orderedMapper).insert(ordered);
    }

    @Test
    void moreThanSixSeatsInOneOrderIsRejected() {
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(futureRecord());

        Ordered ordered = new Ordered();
        ordered.setRecordId(1);
        ordered.setSeat("1排1座,1排2座,1排3座,1排4座,1排5座,1排6座,1排7座");

        assertThatThrownBy(() -> orderedService.createOrder(ordered, RoleEnum.USER.name(), 8))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("最多选择 6 个座位");
    }

    // ========== 余额支付 / 退款入账 / 单价快照 ==========

    /** 下单只锁座不扣款：钱包必须等到支付时才被触碰 */
    @Test
    void createOrderDoesNotTouchWallet() {
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(futureRecord());

        Ordered ordered = new Ordered();
        ordered.setRecordId(1);
        ordered.setSeat("1排1座");
        orderedService.createOrder(ordered, RoleEnum.USER.name(), 100);

        verifyNoInteractions(walletService);
    }

    @Test
    void createOrderSnapshotsUnitPrice() {
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(futureRecord());

        Ordered ordered = new Ordered();
        ordered.setRecordId(1);
        ordered.setSeat("1排1座,1排2座");
        orderedService.createOrder(ordered, RoleEnum.USER.name(), 100);

        verify(orderedMapper).insert(argThat(o ->
                o.getUnitPrice() != null
                        && o.getUnitPrice().compareTo(new BigDecimal("45.00")) == 0
                        && o.getTotal().compareTo(new BigDecimal("90.00")) == 0
        ));
    }

    @Test
    void payOrderDeductsBalanceWithOrderAmount() {
        Ordered ordered = pendingPaymentOrder();
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(purchasableRecord());

        orderedService.payOrder(1, RoleEnum.USER.name(), 100, PAY_PASSWORD);

        verify(walletService).debitPurchase(eq(100), argThat(amountIs("90.00")), eq(1));
    }

    /**
     * 余额不足是本次修复的核心分支：订单必须停在「待支付」且不写任何库
     * （status 与 pending_timeout_at 都不动），座位继续锁定，
     * 用户去充值后可回到订单页继续支付。
     */
    @Test
    void payOrderWhenBalanceInsufficientLeavesOrderPendingAndSeatLocked() {
        Ordered ordered = pendingPaymentOrder();
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(purchasableRecord());
        doThrow(new CustomException(ErrorCode.BUSINESS_CONFLICT, "账户余额不足，请先充值"))
                .when(walletService).debitPurchase(anyInt(), any(), anyInt());

        assertThatThrownBy(() -> orderedService.payOrder(1, RoleEnum.USER.name(), 100, PAY_PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("余额不足");

        verify(orderedMapper, never()).updateById(any());
    }

    /**
     * 支付密码是扣款闸门：校验失败必须一分钱不动、订单状态不动。
     * 这里同时钉住"验密码在扣款之前"这个顺序 —— 若实现把 debitPurchase 提到 verify 前面，
     * 本用例会因为 debitPurchase 被调用而失败。
     */
    @Test
    void payOrderStopsBeforeDebitWhenPaymentPasswordRejected() {
        Ordered ordered = pendingPaymentOrder();
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(purchasableRecord());
        // 校验失败由「返回异常」表达（计数先由它自己的事务提交），这里还原成同样的形态
        when(payPasswordService.checkForPayment(anyInt(), any()))
                .thenReturn(new CustomException(ErrorCode.UNAUTHORIZED, "支付密码错误，还可重试 4 次"));

        assertThatThrownBy(() -> orderedService.payOrder(1, RoleEnum.USER.name(), 100, "000000"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("支付密码错误");

        verify(walletService, never()).debitPurchase(anyInt(), any(), anyInt());
        verify(orderedMapper, never()).updateById(any());
    }

    /**
     * 验的是**订单归属者**的支付密码，不是调用者传入的 id：扣的是 owner 的余额，
     * 闸门就必须落在 owner 的密码上。ADMIN 能通过 ensureOrderAccess，但拿不出别人的密码。
     */
    @Test
    void payOrderVerifiesPaymentPasswordOfOrderOwner() {
        Ordered ordered = pendingPaymentOrder();
        ordered.setUserId(100);
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(purchasableRecord());

        orderedService.payOrder(1, RoleEnum.ADMIN.name(), 999, PAY_PASSWORD);

        verify(payPasswordService).checkForPayment(100, PAY_PASSWORD);
    }

    @Test
    void refundOrderCreditsBalanceWithOrderAmount() {
        Ordered ordered = paidOrder();
        ordered.setStart(minutesFromNow(120));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        orderedService.refundOrder(1, RoleEnum.USER.name(), 100);

        verify(walletService).creditRefund(eq(100), argThat(amountIs("90.00")), eq(1));
    }

    /** 过了退票窗口的订单不得退款：校验必须先于任何资金动作 */
    @Test
    void refundOrderDoesNotCreditWhenDeadlinePassed() {
        Ordered ordered = paidOrder();
        ordered.setStart(minutesFromNow(30));
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.refundOrder(1, RoleEnum.USER.name(), 100))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("退票截止");

        verifyNoInteractions(walletService);
    }

    // ========== 订单物理删除守卫（删除不得再充当免费退票） ==========

    @Test
    void cannotDeletePaidOrder() {
        when(orderedMapper.selectById(1)).thenReturn(orderWithStatus("待取票"));

        assertThatThrownBy(() -> orderedService.deleteScoped(1, RoleEnum.USER.name(), 100))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("已取消 / 已退票");

        verify(orderedMapper, never()).deleteById(anyInt());
    }

    @Test
    void cannotDeletePickedUpOrder() {
        when(orderedMapper.selectById(1)).thenReturn(orderWithStatus("已取票"));

        assertThatThrownBy(() -> orderedService.deleteScoped(1, RoleEnum.USER.name(), 100))
                .isInstanceOf(CustomException.class);

        verify(orderedMapper, never()).deleteById(anyInt());
    }

    @Test
    void cannotDeletePendingPaymentOrder() {
        when(orderedMapper.selectById(1)).thenReturn(orderWithStatus("待支付"));

        assertThatThrownBy(() -> orderedService.deleteScoped(1, RoleEnum.USER.name(), 100))
                .isInstanceOf(CustomException.class);

        verify(orderedMapper, never()).deleteById(anyInt());
    }

    @Test
    void canDeleteCancelledOrder() {
        when(orderedMapper.selectById(1)).thenReturn(orderWithStatus("已取消"));

        orderedService.deleteScoped(1, RoleEnum.USER.name(), 100);

        verify(orderedMapper).deleteById(1);
    }

    @Test
    void canDeleteRefundedOrder() {
        when(orderedMapper.selectById(1)).thenReturn(orderWithStatus("已退票"));

        orderedService.deleteScoped(1, RoleEnum.USER.name(), 100);

        verify(orderedMapper).deleteById(1);
    }

    @Test
    void batchDeleteRejectsWholeBatchWhenAnyOrderIsNotDeletable() {
        when(orderedMapper.selectById(1)).thenReturn(orderWithStatus("已取消"));
        when(orderedMapper.selectById(2)).thenReturn(orderWithStatus("待取票"));

        List<Integer> ids = List.of(1, 2);
        assertThatThrownBy(() -> orderedService.deleteBatchScoped(ids, RoleEnum.USER.name(), 100))
                .isInstanceOf(CustomException.class);

        verify(orderedMapper, never()).deleteBatch(any());
    }

    // ========== 选座图视角：只暴露座位与归属，不泄露他人订单明细 ==========

    /** 他人订单在选座图上只能体现出"这个座位被占"，订单号/用户/金额一概不出参 */
    @Test
    void seatOccupancyHidesOtherUsersOrderDetails() {
        Ordered other = new Ordered();
        other.setId(11);
        other.setUserId(200);
        other.setOrders("20260928DEADBEEF");
        other.setSeat("1排1座");
        other.setStatus("待取票");
        other.setTotal(new BigDecimal("45.00"));
        other.setPendingTimeoutAt("2026-09-28 12:00:00");
        when(orderedMapper.selectActiveByRecordId(1)).thenReturn(List.of(other));

        List<SeatOccupancyView> view = orderedService.selectSeatOccupancy(1, 100);

        assertThat(view).hasSize(1);
        assertThat(view.get(0).getSeat()).isEqualTo("1排1座");
        assertThat(view.get(0).getMine()).isFalse();
        assertThat(view.get(0).getOrderId()).isNull();
        assertThat(view.get(0).getOrders()).isNull();
        assertThat(view.get(0).getStatus()).isNull();
        assertThat(view.get(0).getTotal()).isNull();
        assertThat(view.get(0).getPendingTimeoutAt()).isNull();
    }

    /** 本人订单要带齐字段，否则选座图的"继续支付 / 取消锁座"会失去数据 */
    @Test
    void seatOccupancyExposesOwnOrderDetailsForContinuePayment() {
        Ordered mine = new Ordered();
        mine.setId(11);
        mine.setUserId(100);
        mine.setOrders("20260928DEADBEEF");
        mine.setSeat("1排1座");
        mine.setStatus("待支付");
        mine.setTotal(new BigDecimal("45.00"));
        mine.setPendingTimeoutAt("2026-09-28 12:00:00");
        when(orderedMapper.selectActiveByRecordId(1)).thenReturn(List.of(mine));

        List<SeatOccupancyView> view = orderedService.selectSeatOccupancy(1, 100);

        assertThat(view).hasSize(1);
        assertThat(view.get(0).getSeat()).isEqualTo("1排1座");
        assertThat(view.get(0).getMine()).isTrue();
        assertThat(view.get(0).getOrderId()).isEqualTo(11);
        assertThat(view.get(0).getOrders()).isEqualTo("20260928DEADBEEF");
        assertThat(view.get(0).getStatus()).isEqualTo("待支付");
        assertThat(view.get(0).getTotal()).isEqualByComparingTo("45.00");
        assertThat(view.get(0).getPendingTimeoutAt()).isEqualTo("2026-09-28 12:00:00");
    }

    /** 拿不到 JWT 用户时不得误判成"本人的单" */
    @Test
    void seatOccupancyTreatsNobodyAsOwnWhenTokenUserMissing() {
        Ordered other = new Ordered();
        other.setId(11);
        other.setUserId(100);
        other.setSeat("1排1座");
        when(orderedMapper.selectActiveByRecordId(1)).thenReturn(List.of(other));

        List<SeatOccupancyView> view = orderedService.selectSeatOccupancy(1, null);

        assertThat(view).hasSize(1);
        assertThat(view.get(0).getMine()).isFalse();
        assertThat(view.get(0).getOrderId()).isNull();
    }

    // ========== 取票大厅自助核销 ==========

    @Test
    void payOrderGeneratesPickupCode() {
        when(orderedMapper.selectByIdForUpdate(1)).thenReturn(pendingPaymentOrder());
        when(recordMapper.selectByIdForUpdate(1)).thenReturn(purchasableRecord());

        orderedService.payOrder(1, RoleEnum.USER.name(), 100, PAY_PASSWORD);

        // 字母表刻意剔除 I/L/O/0/1 —— 人工从手机抄到自助机上时这几个最容易看错。
        // 正则写全字母表而不是 [A-Z2-9]，就是为了把"不许出现易混字符"钉住。
        verify(orderedMapper).updateById(argThat(u ->
                u.getPickupCode() != null
                        && u.getPickupCode().matches(
                        "[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}-[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}")
        ));
    }

    @Test
    void redeemByCodeMarksPickedUpAndReturnsVoucher() {
        when(orderedMapper.selectByPickupCode("8F3A-2C71")).thenReturn(redeemableOrder());
        when(orderedMapper.markPickedUpByCode("8F3A-2C71", OrderStatus.PICKED_UP, OrderStatus.PENDING)).thenReturn(1);
        when(filmMapper.selectById(24)).thenReturn(filmRow(24, "流浪地球2", 120));
        when(cinemaMapper.selectById(10)).thenReturn(cinemaRow(10, "奥斯卡赛影城"));
        when(roomMapper.selectById(7)).thenReturn(roomRow(7, "一号厅"));

        // 入参刻意写成小写、去横杠：归一化后才会变成库里存的 8F3A-2C71
        TicketVoucherView voucher = orderedService.redeemByCode("8f3a2c71");

        assertThat(voucher.getFilmTitle()).isEqualTo("流浪地球2");
        assertThat(voucher.getCinemaName()).isEqualTo("奥斯卡赛影城");
        assertThat(voucher.getRoomName()).isEqualTo("一号厅");
        assertThat(voucher.getSeat()).isEqualTo("3排4座");
        assertThat(voucher.getNumber()).isEqualTo(2);
        // 凭条里不存在 orderId / 订单编号 / 金额 / userId 字段 —— 匿名端点靠类型本身保证不泄露，
        // 不需要额外断言（要加字段就必须改 TicketVoucherView，那一步会被人看见）
    }

    /** 长度不符的码直接判无效，不该白跑一次数据库 */
    @Test
    void redeemByCodeRejectsMalformedCodeWithoutQuerying() {
        assertThatThrownBy(() -> orderedService.redeemByCode("123"))
                .isInstanceOf(CustomException.class);
        verify(orderedMapper, never()).selectByPickupCode(any());
    }

    @Test
    void redeemByCodeRejectsUnknownCode() {
        when(orderedMapper.selectByPickupCode("8F3A-2C71")).thenReturn(null);

        assertThatThrownBy(() -> orderedService.redeemByCode("8F3A-2C71"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("无效");
        verify(orderedMapper, never()).markPickedUpByCode(any(), any(), any());
    }

    /**
     * 取票码不可核销的各种情形，统一走「读到的状态/时间不合法 → 拒绝且不改库」。
     * 「已结束」一例把放映时间挪到过去（片长 120 分钟，由 filmMapper 提供），
     * 其余四例走到状态分支即抛，不需片长。
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("unredeemableOrders")
    void redeemByCodeRejectsUnredeemableOrder(String status, Integer startMinutesFromNow, String expectedMessage) {
        Ordered ordered = redeemableOrder();
        ordered.setStatus(status);
        when(orderedMapper.selectByPickupCode("8F3A-2C71")).thenReturn(ordered);
        if (startMinutesFromNow != null) {
            ordered.setStart(minutesFromNow(startMinutesFromNow));
            when(filmMapper.selectById(24)).thenReturn(filmRow(24, "流浪地球2", 120));
        }

        assertThatThrownBy(() -> orderedService.redeemByCode("8F3A-2C71"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(expectedMessage);
        verify(orderedMapper, never()).markPickedUpByCode(any(), any(), any());
    }

    static Stream<Arguments> unredeemableOrders() {
        return Stream.of(
                arguments("已取票", null, "已取出"),
                arguments("已退票", null, "退票"),
                arguments("已取消", null, "取消"),
                arguments("待支付", null, "尚未支付"),
                arguments("待取票", -200, "已结束")
        );
    }

    /**
     * 正面守卫：status 没有库级约束，NULL / 脏值会绕过四条具体分支。
     * 没有这道守卫就会落到条件更新上拿到 0 行，被误报成"该票已取出"。
     */
    @Test
    void redeemByCodeRejectsBlankStatusWithAccurateMessage() {
        Ordered ordered = redeemableOrder();
        ordered.setStatus(null);
        when(orderedMapper.selectByPickupCode("8F3A-2C71")).thenReturn(ordered);

        assertThatThrownBy(() -> orderedService.redeemByCode("8F3A-2C71"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("不可取票");
        verify(orderedMapper, never()).markPickedUpByCode(any(), any(), any());
    }

    /**
     * 并发重复核销：读到的状态是待取票，但改库时被抢先（受影响 0 行）。
     * 这条不靠悲观锁，靠 {@code UPDATE ... WHERE status = '待取票'} 的行数判定 ——
     * 若实现改成"读完直接无条件 update"，本用例会失败。
     */
    @Test
    void redeemByCodeDetectsConcurrentRedemption() {
        when(orderedMapper.selectByPickupCode("8F3A-2C71")).thenReturn(redeemableOrder());
        when(filmMapper.selectById(24)).thenReturn(filmRow(24, "流浪地球2", 120));
        when(orderedMapper.markPickedUpByCode("8F3A-2C71", OrderStatus.PICKED_UP, OrderStatus.PENDING)).thenReturn(0);

        assertThatThrownBy(() -> orderedService.redeemByCode("8F3A-2C71"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("已取出");
    }

    // ========== 今日票房（前台公开只读聚合） ==========

    /**
     * 服务层只是转发：日期边界与统计时刻都由数据库时钟在同一条 SQL 里给出，
     * 这里不得再补一次查询、不得把金额转成 double、不得加额外的派生字段。
     * 谓词本身的正确性（pay_time 取日、status IN 待取票/已取票）Mockito 测不到 ——
     * 打桩之后测的是桩，不是谓词。它只能在「备用端口 + 临时库」上打真实库验证。
     */
    @Test
    void todayPaidRevenuePassesMapperRowThroughUnchanged() {
        TodayRevenueView row = new TodayRevenueView(new BigDecimal("136.50"), "2026-09-29 15:04:23");
        when(orderedMapper.selectTodayPaidRevenue()).thenReturn(row);

        TodayRevenueView result = orderedService.todayPaidRevenue();

        assertThat(result.getTotal()).isEqualByComparingTo("136.50");
        assertThat(result.getUpdatedAt()).isEqualTo("2026-09-29 15:04:23");
        verify(orderedMapper).selectTodayPaidRevenue();
    }

    private Ordered redeemableOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待取票");
        ordered.setPickupCode("8F3A-2C71");
        ordered.setFilmId(24);
        ordered.setCinemaId(10);
        ordered.setRoomId(7);
        ordered.setStart(futureStart());
        ordered.setSeat("3排4座");
        ordered.setNumber(2);
        return ordered;
    }

    private Film filmRow(Integer id, String title, Integer minutes) {
        Film film = new Film();
        film.setId(id);
        film.setTitle(title);
        film.setTime(minutes);
        return film;
    }

    private Cinema cinemaRow(Integer id, String name) {
        Cinema cinema = new Cinema();
        cinema.setId(id);
        cinema.setName(name);
        return cinema;
    }

    private Room roomRow(Integer id, String name) {
        Room room = new Room();
        room.setId(id);
        room.setName(name);
        return room;
    }

    private Ordered orderWithStatus(String status) {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus(status);
        return ordered;
    }

    /** 金额按数值比较，不依赖 BigDecimal 的小数位（90.0 / 90.00 视为同一笔钱） */
    private static ArgumentMatcher<BigDecimal> amountIs(String expected) {
        return amount -> amount != null && amount.compareTo(new BigDecimal(expected)) == 0;
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
        // recordId 必须有值：payOrder 会锁排片行复核「场次是否仍可购」（BL-06），
        // null 会被裁定为「场次已不存在」
        ordered.setRecordId(1);
        ordered.setStatus("待支付");
        ordered.setTotal(new BigDecimal("90.00"));
        // 未超时：留 5 分钟窗口，默认走正常支付路径而不是超时取消
        ordered.setPendingTimeoutAt(minutesFromNow(5));
        return ordered;
    }

    /**
     * 支付路径的排片行桩：可购（未来场次、正常状态）。
     *
     * <p>payOrder 现在会锁排片行复核可购性（BL-06），不配桩就会拿到 null。
     * 正常支付用例统一用它，把"可购"这个前提显式写出来。
     */
    private Record purchasableRecord() {
        Record recordRow = futureRecord();
        recordRow.setStatus(RecordStatus.NORMAL);
        return recordRow;
    }

    private Ordered paidOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(1);
        ordered.setUserId(100);
        ordered.setStatus("待取票");
        ordered.setTotal(new BigDecimal("90.00"));
        return ordered;
    }

    private static String minutesFromNow(int minutes) {
        return LocalDateTime.now().plusMinutes(minutes).withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
