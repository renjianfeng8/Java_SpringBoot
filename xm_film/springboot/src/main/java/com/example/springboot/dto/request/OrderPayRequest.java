package com.example.springboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 支付订单入参。
 *
 * 刻意收的是**订单归属者本人的支付密码**：扣的是订单 owner 的余额（见 WalletService.debitPurchase），
 * 所以闸门也必须是 owner 的密码。ADMIN / CINEMA 虽然能通过 ensureOrderAccess 命中该订单，
 * 但拿不出别人的支付密码，这条通路因此被结构性地关掉，不需要再补一条角色黑名单。
 */
@Data
public class OrderPayRequest {

    @NotBlank(message = "请输入支付密码")
    private String payPassword;
}
