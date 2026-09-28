package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.FundSource;
import com.example.springboot.entity.FundFlow;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FundFlowMapper;
import com.example.springboot.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 账户余额的唯一出处。四条契约：
 *   1. 足额购票 → 扣减 + 一条「购票」流水（负金额、前后余额准确）
 *   2. 余额不足 → 抛业务冲突，不扣款、不记流水
 *   3. 充值回调成功 → 入账 + 一条「充值」流水
 *   4. 退票 → 入账 + 一条「退票」流水
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final Integer USER_ID = 6;

    @Mock
    private UserMapper userMapper;

    @Mock
    private FundFlowMapper fundFlowMapper;

    @InjectMocks
    private WalletService walletService;

    @Test
    void debitPurchase_withSufficientBalance_shouldDeductAndWritePurchaseFlow() {
        when(userMapper.selectBalanceForUpdate(USER_ID)).thenReturn(new BigDecimal("100.00"));
        when(userMapper.deductBalance(USER_ID, new BigDecimal("60.00"))).thenReturn(1);

        walletService.debitPurchase(USER_ID, new BigDecimal("60.00"), 99);

        verify(userMapper).deductBalance(USER_ID, new BigDecimal("60.00"));

        FundFlow flow = captureSingleFlow();
        assertEquals(FundSource.PURCHASE, flow.getSource());
        assertEquals(USER_ID, flow.getUserId());
        assertEquals(0, new BigDecimal("-60.00").compareTo(flow.getChangeAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(flow.getBalanceBefore()));
        assertEquals(0, new BigDecimal("40.00").compareTo(flow.getBalanceAfter()));
        assertEquals(99, flow.getRelatedId());
    }

    @Test
    void debitPurchase_withInsufficientBalance_shouldThrowAndChangeNothing() {
        when(userMapper.selectBalanceForUpdate(USER_ID)).thenReturn(new BigDecimal("10.00"));

        CustomException ex = assertThrows(CustomException.class,
                () -> walletService.debitPurchase(USER_ID, new BigDecimal("60.00"), 99));

        assertEquals(ErrorCode.BUSINESS_CONFLICT.code(), ex.getCode());
        verify(userMapper, never()).deductBalance(anyInt(), any());
        verify(fundFlowMapper, never()).insert(any());
    }

    @Test
    void debitPurchase_whenUserMissing_shouldThrowNotFound() {
        CustomException ex = assertThrows(CustomException.class,
                () -> walletService.debitPurchase(USER_ID, new BigDecimal("60.00"), 99));

        assertEquals(ErrorCode.NOT_FOUND.code(), ex.getCode());
        verify(fundFlowMapper, never()).insert(any());
    }

    @Test
    void debitPurchase_withNonPositiveAmount_shouldThrowAndChangeNothing() {
        assertThrows(CustomException.class,
                () -> walletService.debitPurchase(USER_ID, BigDecimal.ZERO, 99));

        verify(userMapper, never()).selectBalanceForUpdate(anyInt());
        verify(fundFlowMapper, never()).insert(any());
    }

    @Test
    void creditRecharge_shouldAddBalanceAndWriteRechargeFlow() {
        when(userMapper.selectBalanceForUpdate(USER_ID)).thenReturn(new BigDecimal("100.00"));

        walletService.creditRecharge(USER_ID, new BigDecimal("200.00"), 7);

        verify(userMapper).addBalance(USER_ID, new BigDecimal("200.00"));

        FundFlow flow = captureSingleFlow();
        assertEquals(FundSource.RECHARGE, flow.getSource());
        assertEquals(0, new BigDecimal("200.00").compareTo(flow.getChangeAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(flow.getBalanceBefore()));
        assertEquals(0, new BigDecimal("300.00").compareTo(flow.getBalanceAfter()));
        assertEquals(7, flow.getRelatedId());
    }

    @Test
    void creditRecharge_withNonPositiveAmount_shouldThrowAndChangeNothing() {
        assertThrows(CustomException.class,
                () -> walletService.creditRecharge(USER_ID, new BigDecimal("-50.00"), 7));

        verify(userMapper, never()).selectBalanceForUpdate(anyInt());
        verify(fundFlowMapper, never()).insert(any());
    }

    @Test
    void creditRefund_shouldAddBalanceAndWriteRefundFlow() {
        when(userMapper.selectBalanceForUpdate(USER_ID)).thenReturn(new BigDecimal("40.00"));

        walletService.creditRefund(USER_ID, new BigDecimal("60.00"), 99);

        verify(userMapper).addBalance(USER_ID, new BigDecimal("60.00"));

        FundFlow flow = captureSingleFlow();
        assertEquals(FundSource.REFUND, flow.getSource());
        assertEquals(0, new BigDecimal("60.00").compareTo(flow.getChangeAmount()));
        assertEquals(0, new BigDecimal("40.00").compareTo(flow.getBalanceBefore()));
        assertEquals(0, new BigDecimal("100.00").compareTo(flow.getBalanceAfter()));
        assertEquals(99, flow.getRelatedId());
    }

    @Test
    void getBalance_shouldReturnMapperValue() {
        when(userMapper.selectBalance(USER_ID)).thenReturn(new BigDecimal("100.00"));

        assertEquals(0, new BigDecimal("100.00").compareTo(walletService.getBalance(USER_ID)));
    }

    private FundFlow captureSingleFlow() {
        ArgumentCaptor<FundFlow> captor = ArgumentCaptor.forClass(FundFlow.class);
        verify(fundFlowMapper).insert(captor.capture());
        return captor.getValue();
    }
}
