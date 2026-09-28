package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RechargeStatus;
import com.example.springboot.entity.RechargeOrder;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.RechargeOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 充值单据：提交申请只生成「处理中」单据且不改变余额；
 * 模拟支付网关回调成功后置「已完成」并入账，失败置「已失败」且余额不变。
 */
@Service
public class RechargeService {

    /** 单笔充值上限，防止演示时误输入天文数字 */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("50000.00");

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Resource
    private RechargeOrderMapper rechargeOrderMapper;

    @Resource
    private WalletService walletService;

    /**
     * 提交充值申请。只登记单据，余额分文不动 —— 只有回调成功才入账。
     */
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder createRecharge(Integer tokenUserId, String role, BigDecimal amount) {
        if (role != null && !"USER".equals(role)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "仅普通用户可发起充值");
        }
        if (tokenUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        RechargeOrder order = new RechargeOrder();
        order.setRechargeNo(generateRechargeNo());
        order.setUserId(tokenUserId);
        order.setAmount(normalizeAmount(amount));
        order.setStatus(RechargeStatus.PROCESSING);
        rechargeOrderMapper.insert(order);
        return order;
    }

    /** 充值单据查询：USER 只看自己的，ADMIN 看全部 */
    public List<RechargeOrder> selectScoped(RechargeOrder query, String role, Integer tokenUserId) {
        RechargeOrder condition = query == null ? new RechargeOrder() : query;
        if ("ADMIN".equals(role)) {
            return rechargeOrderMapper.selectAll(condition);
        }
        if ("USER".equals(role)) {
            condition.setUserId(tokenUserId);
            return rechargeOrderMapper.selectAll(condition);
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "无权查看充值单据");
    }

    /**
     * 模拟第三方支付网关回调。演示环境由前端按钮触发，正式环境即支付平台异步回调入口。
     * 只有「处理中」单据可被处理，已完成/已失败再次回调一律拒绝（幂等）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleCallback(Integer id, boolean success, String remark, String role, Integer tokenUserId) {
        RechargeOrder order = rechargeOrderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "充值单据不存在");
        }
        if (!"ADMIN".equals(role)
                && !("USER".equals(role) && tokenUserId != null && tokenUserId.equals(order.getUserId()))) {
            throw new CustomException(ErrorCode.FORBIDDEN, "无权操作该充值单据");
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
        return LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    /** 充值单号：R + 日期 + 8 位随机，与订单号一眼可分 */
    private String generateRechargeNo() {
        String date = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "R" + date + suffix;
    }
}
