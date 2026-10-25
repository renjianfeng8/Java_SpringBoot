package com.example.springboot.service;

import com.example.springboot.common.OrderNumber;
import com.example.springboot.common.SystemClock;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RechargeStatus;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.RechargeOrder;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.RechargeOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 充值单据：提交申请只生成「处理中」单据且不改变余额；
 * 模拟支付网关回调成功后置「已完成」并入账，失败置「已失败」且余额不变。
 */
@Service
public class RechargeService {

    /** 单笔充值上限，防止演示时误输入天文数字 */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("50000.00");

    private final RechargeOrderMapper rechargeOrderMapper;

    private final WalletService walletService;

    public RechargeService(RechargeOrderMapper rechargeOrderMapper, WalletService walletService) {
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.walletService = walletService;
    }

    /**
     * 提交充值申请。只登记单据，余额分文不动 —— 只有回调成功才入账。
     */
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder createRecharge(Integer tokenUserId, String role, BigDecimal amount) {
        if (role != null && !RoleEnum.USER.name().equals(role)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "仅普通用户可发起充值");
        }
        if (tokenUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        RechargeOrder order = new RechargeOrder();
        // R 前缀让充值单号与订单号一眼可分；单号生成规则统一在 OrderNumber 里
        order.setRechargeNo(OrderNumber.generate("R"));
        order.setUserId(tokenUserId);
        order.setAmount(normalizeAmount(amount));
        order.setStatus(RechargeStatus.PROCESSING);
        rechargeOrderMapper.insert(order);
        return order;
    }

    /** 充值单据查询：USER 只看自己的，ADMIN 看全部 */
    public List<RechargeOrder> selectScoped(RechargeOrder query, String role, Integer tokenUserId) {
        RechargeOrder condition = query == null ? new RechargeOrder() : query;
        if (RoleEnum.ADMIN.name().equals(role)) {
            return rechargeOrderMapper.selectAll(condition);
        }
        if (RoleEnum.USER.name().equals(role)) {
            condition.setUserId(tokenUserId);
            return rechargeOrderMapper.selectAll(condition);
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "无权查看充值单据");
    }

    /**
     * 管理员审核充值单据：通过则入账，拒绝则置为失败。
     *
     * <p>这个端点曾经允许 USER 回调自己的单据，而 {@code success} 完全取自请求体，
     * 等于任意用户可给自己充值（SEC-01）。在接入真实支付网关的验签/查单之前，
     * 唯一安全的入口是管理员人工审核 —— 用户只能提交申请，不能确认到账。
     *
     * <p>只有「处理中」单据可被处理，已完成/已失败再次回调一律拒绝（幂等）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleCallback(Integer id, boolean success, String remark, String role) {
        // 先判角色再查库：非管理员既拿不到单据内容，也无法用 404/403 的差异探测 id 是否存在
        if (!RoleEnum.ADMIN.name().equals(role)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "仅管理员可审核充值单据");
        }
        RechargeOrder order = rechargeOrderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "充值单据不存在");
        }
        if (!RechargeStatus.PROCESSING.equals(order.getStatus())) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "该充值单据已处理，不可重复回调");
        }

        RechargeOrder update = new RechargeOrder();
        update.setId(id);
        update.setFinishTime(now());
        if (success) {
            update.setStatus(RechargeStatus.SUCCESS);
            rechargeOrderMapper.updateById(update);
            walletService.creditRecharge(order.getUserId(), order.getAmount(), id);
        } else {
            update.setStatus(RechargeStatus.FAILED);
            update.setRemark(remark == null || remark.isBlank() ? "模拟支付回调失败" : remark);
            rechargeOrderMapper.updateById(update);
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "充值金额必须大于 0");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(MAX_AMOUNT) > 0) {
            throw new CustomException(ErrorCode.PARAM_INVALID,
                    "单笔充值金额不得超过 " + MAX_AMOUNT.toPlainString() + " 元");
        }
        return normalized;
    }

    private String now() {
        return SystemClock.nowFormatted();
    }
}
