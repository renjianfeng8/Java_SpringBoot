package com.example.springboot;

import com.example.springboot.common.enums.OrderStatus;
import com.example.springboot.common.enums.PayResult;
import com.example.springboot.common.enums.RecordStatus;
import com.example.springboot.entity.Ordered;
import com.example.springboot.entity.Record;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.mapper.RoomMapper;
import com.example.springboot.service.OrderedService;
import com.example.springboot.service.PayPasswordService;
import com.example.springboot.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * BL-06 守卫：<b>支付前必须复核场次是否仍可购</b>。
 *
 * <p>背景（2026-10-19 实测）：{@code insertOrder} 判过 {@code isPurchasable}，但
 * {@code payOrder} 完全不看场次。建单到支付之间最长 5 分钟，而<b>场次不需要任何人
 * 退票就会自己开演</b>。实测确认为已开演场次出票成功（状态变「待取票」并生成取票码），
 * 此时退票截止（映前 60 分钟）早已过去，钱扣了、票废了、退不了。
 *
 * <p><b>为什么用 Mockito</b>：本类要断言三件事——(1) 复核发生在扣款之前；
 * (2) 复核走的是<b>锁定读</b> {@code selectByIdForUpdate} 而不是普通读；
 * (3) 被拒时一分钱没动。真实数据库只能验证「过去场次被拒」——把复核去掉后，
 * 那种测试在串行场景下依然会红，但它无法证明「先复核后扣款」与「用的是锁定读」。
 */
@ExtendWith(MockitoExtension.class)
class PayOrderPurchasableGuardTest {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Mock
    OrderedMapper orderedMapper;

    @Mock
    RecordMapper recordMapper;

    @Mock
    FilmMapper filmMapper;

    @Mock
    RoomMapper roomMapper;

    @Mock
    CinemaMapper cinemaMapper;

    @Mock
    WalletService walletService;

    @Mock
    PayPasswordService payPasswordService;

    private OrderedService orderedService;

    @BeforeEach
    void setUp() {
        orderedService = new OrderedService(orderedMapper, recordMapper, filmMapper,
                roomMapper, cinemaMapper, walletService, payPasswordService);
    }

    private static String format(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    /** 一笔仍在锁座窗口内（未超时）的待支付订单，属于用户 9 */
    private Ordered pendingOrder() {
        Ordered ordered = new Ordered();
        ordered.setId(100);
        ordered.setRecordId(7);
        ordered.setUserId(9);
        ordered.setStatus(OrderStatus.PENDING_PAYMENT);
        ordered.setTotal(45.0);
        ordered.setPendingTimeoutAt(format(LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(5)));
        return ordered;
    }

    private Record recordStarting(LocalDateTime start) {
        Record recordItem = new Record();
        recordItem.setId(7);
        recordItem.setStatus(RecordStatus.NORMAL);
        recordItem.setStart(format(start));
        return recordItem;
    }

    /**
     * 场次已开演 → 拒绝支付，且<b>不扣款</b>。
     *
     * <p>这是 BL-06 的核心断言。只断言"抛异常"不够：若复核写在扣款之后，
     * 异常会回滚整笔事务，串行测试照样全绿，但并发下"扣款成功再报错"的中间态
     * 会短暂可见。这里直接盯 walletService 从未被调用。
     *
     * <p>同时断言密码校验也没被调用：复核排在验密之前，
     * 场次不可购时不该白白消耗用户一次密码尝试次数。
     */
    @Test
    void rejectsPaymentForStartedRecordAndNeverDebits() {
        when(orderedMapper.selectByIdForUpdate(100)).thenReturn(pendingOrder());
        when(recordMapper.selectByIdForUpdate(7))
                .thenReturn(recordStarting(LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(1)));

        assertThatThrownBy(() -> orderedService.payOrder(100, "USER", 9, "123456"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("已开场");

        verifyNoInteractions(walletService);
        verifyNoInteractions(payPasswordService);
        // 订单不得离开待支付
        verify(orderedMapper, never()).updateById(any());
    }

    /** 场次已人工停售 → 同样拒绝，且不扣款 */
    @Test
    void rejectsPaymentForStoppedRecord() {
        when(orderedMapper.selectByIdForUpdate(100)).thenReturn(pendingOrder());
        Record stopped = recordStarting(LocalDateTime.now(ZoneId.systemDefault()).plusHours(2));
        stopped.setStatus(RecordStatus.STOPPED);
        when(recordMapper.selectByIdForUpdate(7)).thenReturn(stopped);

        assertThatThrownBy(() -> orderedService.payOrder(100, "USER", 9, "123456"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("停售");

        verifyNoInteractions(walletService);
    }

    /**
     * 复核必须用<b>锁定读</b>。
     *
     * <p>与 BL-01 同一个教训：普通 {@code selectById} 在 REPEATABLE-READ 下读到的是
     * 事务开始时的快照。管理员若把场次改到过去或停售，普通读看不到，复核形同虚设。
     * 锁排片行还与 {@code addScoped/updateScoped/deleteScoped} 抢同一把锁（BL-02）。
     */
    @Test
    void recheckUsesLockingRead() {
        when(orderedMapper.selectByIdForUpdate(100)).thenReturn(pendingOrder());
        when(recordMapper.selectByIdForUpdate(7))
                .thenReturn(recordStarting(LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(1)));

        assertThatThrownBy(() -> orderedService.payOrder(100, "USER", 9, "123456"))
                .isInstanceOf(CustomException.class);

        verify(recordMapper).selectByIdForUpdate(eq(7));
        verify(recordMapper, never()).selectById(anyInt());
    }

    /** 场次已被删除 → 给出可读提示，而不是空指针或「系统错误」 */
    @Test
    void rejectsPaymentWhenRecordMissing() {
        when(orderedMapper.selectByIdForUpdate(100)).thenReturn(pendingOrder());
        when(recordMapper.selectByIdForUpdate(7)).thenReturn(null);

        assertThatThrownBy(() -> orderedService.payOrder(100, "USER", 9, "123456"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("场次已不存在");

        verifyNoInteractions(walletService);
    }

    /**
     * 场次仍可购 → 正常支付，且复核在扣款<b>之前</b>完成。
     *
     * <p>验证"先复核后扣款"用 InOrder：单靠"最终扣了钱"无法区分两种顺序。
     */
    @Test
    void paysWhenRecordStillPurchasable() {
        when(orderedMapper.selectByIdForUpdate(100)).thenReturn(pendingOrder());
        when(recordMapper.selectByIdForUpdate(7))
                .thenReturn(recordStarting(LocalDateTime.now(ZoneId.systemDefault()).plusHours(2)));
        when(payPasswordService.checkForPayment(anyInt(), anyString())).thenReturn(null);

        PayResult result = orderedService.payOrder(100, "USER", 9, "123456");

        assertThat(result).isEqualTo(PayResult.PAID);
        verify(walletService).debitPurchase(eq(9), any(), eq(100));
        verify(orderedMapper).updateById(any());
    }
}
