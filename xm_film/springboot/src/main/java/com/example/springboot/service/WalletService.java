package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.FundSource;
import com.example.springboot.entity.FundFlow;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FundFlowMapper;
import com.example.springboot.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 账户余额的读写唯一出处：行锁读余额 → 校验/变更 → 写一条资金流水，
 * 全部在同一事务内完成。充值入账、购票扣款、退票退款三条路径都走这里，
 * 避免同一套金额逻辑在多处各写一遍。
 */
@Service
public class WalletService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private FundFlowMapper fundFlowMapper;

    /** 充值入账（充值单据回调成功时调用） */
    @Transactional(rollbackFor = Exception.class)
    public void creditRecharge(Integer userId, BigDecimal amount, Integer rechargeOrderId) {
        credit(userId, amount, FundSource.RECHARGE, rechargeOrderId);
    }

    /** 购票扣款；余额不足抛业务冲突，订单与座位都不受影响 */
    @Transactional(rollbackFor = Exception.class)
    public void debitPurchase(Integer userId, BigDecimal amount, Integer orderId) {
        requirePositive(amount);
        BigDecimal before = requireBalanceForUpdate(userId);
        if (before.compareTo(amount) < 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "账户余额不足，请先充值");
        }
        if (userMapper.deductBalance(userId, amount) == 0) {
            // 行锁读到的余额本该足够；走到这里说明余额被绕过本服务的路径改动过。
            // 兜底拒绝，宁可支付失败也不能把余额扣成负数。
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "账户余额不足，请先充值");
        }
        writeFlow(userId, FundSource.PURCHASE, amount.negate(), before, before.subtract(amount), orderId);
    }

    /** 退票退款入账 */
    @Transactional(rollbackFor = Exception.class)
    public void creditRefund(Integer userId, BigDecimal amount, Integer orderId) {
        credit(userId, amount, FundSource.REFUND, orderId);
    }

    /** 账户余额（展示用，不加锁） */
    public BigDecimal getBalance(Integer userId) {
        BigDecimal balance = userMapper.selectBalance(userId);
        if (balance == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return balance;
    }

    private void credit(Integer userId, BigDecimal amount, String source, Integer relatedId) {
        requirePositive(amount);
        BigDecimal before = requireBalanceForUpdate(userId);
        userMapper.addBalance(userId, amount);
        writeFlow(userId, source, amount, before, before.add(amount), relatedId);
    }

    private BigDecimal requireBalanceForUpdate(Integer userId) {
        BigDecimal balance = userMapper.selectBalanceForUpdate(userId);
        if (balance == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return balance;
    }

    /** 金额必须是正数：负数入账会凭空造钱，负数扣款会把扣款变成加钱 */
    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "金额必须大于 0");
        }
    }

    private void writeFlow(Integer userId, String source, BigDecimal changeAmount,
                           BigDecimal before, BigDecimal after, Integer relatedId) {
        FundFlow flow = new FundFlow();
        flow.setUserId(userId);
        flow.setSource(source);
        flow.setChangeAmount(changeAmount);
        flow.setBalanceBefore(before);
        flow.setBalanceAfter(after);
        flow.setRelatedId(relatedId);
        fundFlowMapper.insert(flow);
    }
}
