package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RechargeStatus;
import com.example.springboot.entity.RechargeOrder;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.RechargeOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 充值单据状态机。四条契约：
 *   1. 提交申请 → 只生成「处理中」单据，余额分文不动
 *   2. 回调成功 → 单据「已完成」并入账
 *   3. 回调失败 → 单据「已失败」，余额不变
 *   4. 已处理单据不可重复回调（幂等）
 */
@ExtendWith(MockitoExtension.class)
class RechargeServiceTest {

    private static final Integer USER_ID = 6;

    @Mock
    private RechargeOrderMapper rechargeOrderMapper;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private RechargeService rechargeService;

    @Test
    void createRecharge_shouldCreateProcessingOrderWithoutTouchingBalance() {
        RechargeOrder created = rechargeService.createRecharge(USER_ID, "USER", new BigDecimal("200.00"));

        ArgumentCaptor<RechargeOrder> captor = ArgumentCaptor.forClass(RechargeOrder.class);
        verify(rechargeOrderMapper).insert(captor.capture());
        RechargeOrder saved = captor.getValue();

        assertEquals(RechargeStatus.PROCESSING, saved.getStatus());
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(0, new BigDecimal("200.00").compareTo(saved.getAmount()));
        assertNotNull(saved.getRechargeNo());
        assertFalse(saved.getRechargeNo().isBlank());
        assertSame(saved, created);

        // 关键：提交单据不得入账
        verifyNoInteractions(walletService);
    }

    @Test
    void createRecharge_withNonUserRole_shouldThrow() {
        CustomException ex = assertThrows(CustomException.class,
                () -> rechargeService.createRecharge(USER_ID, "CINEMA", new BigDecimal("200.00")));

        assertEquals(ErrorCode.FORBIDDEN.code(), ex.getCode());
        verify(rechargeOrderMapper, never()).insert(any());
        verifyNoInteractions(walletService);
    }

    @Test
    void createRecharge_withNonPositiveAmount_shouldThrow() {
        CustomException ex = assertThrows(CustomException.class,
                () -> rechargeService.createRecharge(USER_ID, "USER", BigDecimal.ZERO));

        assertEquals(ErrorCode.PARAM_INVALID.code(), ex.getCode());
        verify(rechargeOrderMapper, never()).insert(any());
    }

    @Test
    void createRecharge_withAmountAboveLimit_shouldThrow() {
        CustomException ex = assertThrows(CustomException.class,
                () -> rechargeService.createRecharge(USER_ID, "USER", new BigDecimal("50000.01")));

        assertEquals(ErrorCode.PARAM_INVALID.code(), ex.getCode());
        verify(rechargeOrderMapper, never()).insert(any());
    }

    @Test
    void handleCallback_success_shouldMarkDoneAndCreditBalance() {
        when(rechargeOrderMapper.selectByIdForUpdate(7)).thenReturn(processingOrder());

        rechargeService.handleCallback(7, true, null, "USER", USER_ID);

        verify(walletService).creditRecharge(USER_ID, new BigDecimal("200.00"), 7);

        RechargeOrder update = captureUpdate();
        assertEquals(RechargeStatus.SUCCESS, update.getStatus());
        assertNotNull(update.getFinishTime());
    }

    @Test
    void handleCallback_failure_shouldMarkFailedWithoutCrediting() {
        when(rechargeOrderMapper.selectByIdForUpdate(7)).thenReturn(processingOrder());

        rechargeService.handleCallback(7, false, "余额不足", "USER", USER_ID);

        verifyNoInteractions(walletService);

        RechargeOrder update = captureUpdate();
        assertEquals(RechargeStatus.FAILED, update.getStatus());
        assertEquals("余额不足", update.getRemark());
        assertNotNull(update.getFinishTime());
    }

    @Test
    void handleCallback_onAlreadyDoneOrder_shouldThrow() {
        RechargeOrder done = processingOrder();
        done.setStatus(RechargeStatus.SUCCESS);
        when(rechargeOrderMapper.selectByIdForUpdate(7)).thenReturn(done);

        CustomException ex = assertThrows(CustomException.class,
                () -> rechargeService.handleCallback(7, true, null, "USER", USER_ID));

        assertEquals(ErrorCode.BUSINESS_CONFLICT.code(), ex.getCode());
        verify(walletService, never()).creditRecharge(anyInt(), any(), anyInt());
        verify(rechargeOrderMapper, never()).updateById(any());
    }

    @Test
    void handleCallback_onAlreadyFailedOrder_shouldThrow() {
        RechargeOrder failed = processingOrder();
        failed.setStatus(RechargeStatus.FAILED);
        when(rechargeOrderMapper.selectByIdForUpdate(7)).thenReturn(failed);

        assertThrows(CustomException.class,
                () -> rechargeService.handleCallback(7, true, null, "USER", USER_ID));

        verify(walletService, never()).creditRecharge(anyInt(), any(), anyInt());
    }

    @Test
    void handleCallback_whenNotOwnerAndNotAdmin_shouldThrow() {
        when(rechargeOrderMapper.selectByIdForUpdate(7)).thenReturn(processingOrder());

        CustomException ex = assertThrows(CustomException.class,
                () -> rechargeService.handleCallback(7, true, null, "USER", 999));

        assertEquals(ErrorCode.FORBIDDEN.code(), ex.getCode());
        verify(walletService, never()).creditRecharge(anyInt(), any(), anyInt());
        verify(rechargeOrderMapper, never()).updateById(any());
    }

    @Test
    void handleCallback_whenOrderMissing_shouldThrowNotFound() {
        CustomException ex = assertThrows(CustomException.class,
                () -> rechargeService.handleCallback(7, true, null, "USER", USER_ID));

        assertEquals(ErrorCode.NOT_FOUND.code(), ex.getCode());
        verify(walletService, never()).creditRecharge(anyInt(), any(), anyInt());
    }

    @Test
    void selectScoped_asUser_shouldForceOwnUserIdIgnoringRequestParam() {
        RechargeOrder query = new RechargeOrder();
        query.setUserId(999);
        when(rechargeOrderMapper.selectAll(any())).thenReturn(List.of());

        rechargeService.selectScoped(query, "USER", USER_ID);

        assertEquals(USER_ID, captureCondition().getUserId());
    }

    @Test
    void selectScoped_asAdmin_shouldNotFilterByUser() {
        when(rechargeOrderMapper.selectAll(any())).thenReturn(List.of());

        rechargeService.selectScoped(null, "ADMIN", 1);

        assertNull(captureCondition().getUserId());
    }

    @Test
    void selectScoped_asCinema_shouldThrowForbidden() {
        CustomException ex = assertThrows(CustomException.class,
                () -> rechargeService.selectScoped(null, "CINEMA", 5));

        assertEquals(ErrorCode.FORBIDDEN.code(), ex.getCode());
        verify(rechargeOrderMapper, never()).selectAll(any());
    }

    private RechargeOrder captureCondition() {
        ArgumentCaptor<RechargeOrder> captor = ArgumentCaptor.forClass(RechargeOrder.class);
        verify(rechargeOrderMapper).selectAll(captor.capture());
        return captor.getValue();
    }

    private RechargeOrder processingOrder() {
        RechargeOrder order = new RechargeOrder();
        order.setId(7);
        order.setRechargeNo("R20260928ABCD1234");
        order.setUserId(USER_ID);
        order.setAmount(new BigDecimal("200.00"));
        order.setStatus(RechargeStatus.PROCESSING);
        return order;
    }

    private RechargeOrder captureUpdate() {
        ArgumentCaptor<RechargeOrder> captor = ArgumentCaptor.forClass(RechargeOrder.class);
        verify(rechargeOrderMapper).updateById(captor.capture());
        return captor.getValue();
    }
}
