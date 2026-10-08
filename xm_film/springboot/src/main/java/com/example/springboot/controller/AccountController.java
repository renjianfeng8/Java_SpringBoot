package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.dto.request.PayPasswordChangeRequest;
import com.example.springboot.dto.request.PayPasswordResetRequest;
import com.example.springboot.dto.request.PayPasswordVerifyLoginRequest;
import com.example.springboot.dto.request.PayPasswordVerifyOldRequest;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.PayPasswordService;
import com.example.springboot.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 我的账户。余额与支付密码都只对本人可见：这里按 JWT 返回当前用户的这两项，
 * 而 {@code /api/v1/users} 是 SELECT * 的通用查询，因此它们都不挂在 User 实体上。
 *
 * 支付密码的两个写入口分工固定：
 * {@code /pay-password} 改（验原支付密码）、{@code /pay-password/reset} 设（验登录密码，
 * 兼"忘记支付密码"的自救路径）。两者都不收 userId，归属一律取自 JWT。
 */
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final WalletService walletService;
    private final PayPasswordService payPasswordService;

    public AccountController(WalletService walletService, PayPasswordService payPasswordService) {
        this.walletService = walletService;
        this.payPasswordService = payPasswordService;
    }

    @GetMapping("/summary")
    public Result summary() {
        Integer userId = requireUser();
        Map<String, Object> data = new HashMap<>();
        data.put("balance", walletService.getBalance(userId));
        data.put("hasPayPassword", payPasswordService.hasPayPassword(userId));
        return Result.success(data);
    }

    @PostMapping("/pay-password/verify-old")
    public Result verifyOldPassword(@Valid @RequestBody PayPasswordVerifyOldRequest request) {
        Integer userId = requireUser();
        payPasswordService.verifyOldPassword(userId, request.getOldPassword());
        return Result.success();
    }

    @PostMapping("/pay-password/verify-login")
    public Result verifyLoginPassword(@Valid @RequestBody PayPasswordVerifyLoginRequest request) {
        Integer userId = requireUser();
        payPasswordService.verifyLoginPassword(userId, request.getLoginPassword());
        return Result.success();
    }

    @PutMapping("/pay-password")
    public Result changePayPassword(@Valid @RequestBody PayPasswordChangeRequest request) {
        Integer userId = requireUser();
        payPasswordService.changePayPassword(userId, request.getOldPassword(), request.getPayPassword());
        return Result.success();
    }

    @PutMapping("/pay-password/reset")
    public Result resetPayPassword(@Valid @RequestBody PayPasswordResetRequest request) {
        Integer userId = requireUser();
        payPasswordService.resetWithLoginPassword(userId, request.getLoginPassword(), request.getPayPassword());
        return Result.success();
    }

    /** admin / cinema 表与 user 表是两套主体，只有 USER 有账户余额与支付密码 */
    private Integer requireUser() {
        if (!"USER".equals(AuthContext.role())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "仅普通用户有账户余额");
        }
        return AuthContext.userId();
    }
}
