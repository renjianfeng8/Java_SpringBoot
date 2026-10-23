package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.request.AdminPasswordResetRequest;
import com.example.springboot.dto.request.LoginRequest;
import com.example.springboot.dto.request.PasswordChangeRequest;
import com.example.springboot.dto.request.RegisterRequest;
import com.example.springboot.dto.response.AccountView;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    private final JwtUtils jwtUtils;

    public AuthController(AuthService authService, JwtUtils jwtUtils) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public Result<AccountView> login(@Valid @RequestBody LoginRequest request) {
        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setPassword(request.getPassword());
        account.setRole(request.getRole());
        Account result = authService.login(account);
        String token = jwtUtils.generateToken(result.getId(), result.getRole());
        result.setToken(token);
        return Result.success(toAccountView(result));
    }

    @PostMapping("/register")
    public Result<Object> register(@Valid @RequestBody RegisterRequest request) {
        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setPassword(request.getPassword());
        account.setRole(request.getRole());
        User user = authService.register(account);
        return user == null ? Result.<Object>success() : Result.success(Map.of("id", user.getId()));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordChangeRequest request, HttpServletRequest httpRequest) {
        Account account = new Account();
        account.setPassword(request.getPassword());
        account.setNewPassword(request.getNewPassword());
        String userId = (String) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        if (userId != null) {
            account.setId(Integer.valueOf(userId));
        }
        account.setRole(role);
        authService.updatePassword(account);
        return Result.success();
    }

    @PutMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody AdminPasswordResetRequest request) {
        // /auth/** 不在拦截器的匿名白名单里，但角色仍由本方法兜底：
        // 任何已登录角色都能到达控制器，不能只靠前端隐藏按钮。
        if (!RoleEnum.ADMIN.name().equals(AuthContext.role())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
        }
        authService.resetPassword(request.getRole(), request.getId(), request.getNewPassword(),
                AuthContext.userId());
        return Result.success();
    }

    @GetMapping("/me")
    public Result<AccountView> me(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        Account account = authService.me(role, userId);
        account.setPassword(null);
        return Result.success(toAccountView(account));
    }

    @GetMapping("/years")
    public Result<List<Integer>> getYear() {
        int currentYear = LocalDate.now(ZoneId.systemDefault()).getYear();
        List<Integer> yearList = IntStream.iterate(currentYear, year -> year - 1)
                .limit(11)
                .boxed()
                .toList();
        return Result.success(yearList);
    }

    /** 主体（User / Admin / Cinema）→ 登录态视图：字段并集拷贝，password/newPassword/token 不在视图内。 */
    private AccountView toAccountView(Account account) {
        AccountView view = new AccountView();
        BeanUtils.copyProperties(account, view);
        return view;
    }
}
