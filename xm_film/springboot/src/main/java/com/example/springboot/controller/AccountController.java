package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 我的账户。余额只对本人可见：这里按 JWT 返回当前用户的余额，
 * 而 {@code /api/v1/users} 是 SELECT * 的通用查询，因此余额不挂在 User 实体上。
 */
@Tag(name = "我的账户", description = "当前登录用户的账户余额")
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final WalletService walletService;

    public AccountController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(summary = "账户余额摘要")
    @GetMapping("/summary")
    public Result summary() {
        // admin / cinema 表与 user 表是两套主体，只有 USER 有账户余额
        if (!"USER".equals(AuthContext.role())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "仅普通用户有账户余额");
        }
        return Result.success(Map.of("balance", walletService.getBalance(AuthContext.userId())));
    }
}
