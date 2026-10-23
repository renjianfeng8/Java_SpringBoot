package com.example.springboot.common;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;

import java.util.List;

/**
 * 批量删除的“不能删”汇总。
 *
 * <p>批量删除保持全有或全无（避免删一半后留下不可解释的中间态），但原实现遇到第一条
 * 不能删的记录就抛出，管理员无法知道选中的几十条里究竟是哪条挡住了。
 * 这里把逐条的 BUSINESS_CONFLICT 收集起来，一次性告诉管理员哪些行需要先处理。
 *
 * <p>权限不足 / 记录不存在由调用方原样抛出，不与本提示混在一起。
 */
public final class BatchDeleteGuard {

    /** 消息里最多列出的行数：全列会把 100 条的选择变成一屏弹窗 */
    private static final int MAX_LISTED = 5;

    private BatchDeleteGuard() {
    }

    public static boolean isBlocked(CustomException e) {
        return ErrorCode.BUSINESS_CONFLICT.code().equals(e.getCode());
    }

    public static void throwIfBlocked(List<String> blocked) {
        if (blocked.isEmpty()) {
            return;
        }
        int shown = Math.min(MAX_LISTED, blocked.size());
        StringBuilder message = new StringBuilder("整批未删除，以下 ")
                .append(blocked.size())
                .append(" 条需先处理：")
                .append(String.join("；", blocked.subList(0, shown)));
        if (blocked.size() > shown) {
            message.append("；另有 ").append(blocked.size() - shown).append(" 条");
        }
        throw new CustomException(ErrorCode.BUSINESS_CONFLICT, message.toString());
    }
}
