package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.request.LoginRequest;
import com.example.springboot.dto.request.PasswordChangeRequest;
import com.example.springboot.dto.request.RegisterRequest;
import com.example.springboot.entity.Account;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.CinemaService;
import com.example.springboot.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Tag(name = "认证管理", description = "登录、注册、密码修改、年份列表查询")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Resource
    private AdminService adminService;

    @Resource
    private CinemaService cinemaService;

    @Resource
    private UserService userService;

    @Resource
    private JwtUtils jwtUtils;

    @Operation(summary = "用户登录", description = "三端共用登录接口，根据角色(ADMIN/CINEMA/USER)路由到不同服务")
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginRequest request) {
        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setPassword(request.getPassword());
        account.setRole(request.getRole());
        Account result = null;
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            result = adminService.login(account);
        } else if (RoleEnum.CINEMA.name().equals(account.getRole())) {
            result = cinemaService.login(account);
        } else if (RoleEnum.USER.name().equals(account.getRole())) {
            result = userService.login(account);
        }
        if (result == null) {
            return Result.error(ErrorCode.PARAM_INVALID.code(), "无效的角色类型");
        }
        String token = jwtUtils.generateToken(result.getId(), result.getRole());
        result.setToken(token);
        return Result.success(result);
    }

    @Operation(summary = "用户注册", description = "支持CINEMA(影院)和USER(用户)注册")
    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterRequest request) {
        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setPassword(request.getPassword());
        account.setRole(request.getRole());
        if (RoleEnum.CINEMA.name().equals(account.getRole())) {
            cinemaService.register(account);
        } else if (RoleEnum.USER.name().equals(account.getRole())) {
            userService.register(account);
        } else {
            throw new CustomException(ErrorCode.PARAM_INVALID, "无效的角色类型");
        }
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result updatePassword(@Valid @RequestBody PasswordChangeRequest request, HttpServletRequest httpRequest) {
        Account account = new Account();
        account.setPassword(request.getPassword());
        account.setNewPassword(request.getNewPassword());
        String userId = (String) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        if (userId != null) {
            account.setId(Integer.valueOf(userId));
        }
        account.setRole(role);
        if ("ADMIN".equals(role)) {
            adminService.updatePassword(account);
        } else if ("CINEMA".equals(role)) {
            cinemaService.updatePassword(account);
        } else if ("USER".equals(role)) {
            userService.updatePassword(account);
        } else {
            throw new CustomException(ErrorCode.SYSTEM_ERROR, "非法输入");
        }
        return Result.success();
    }

    @Operation(summary = "获取年份列表", description = "用于前端搜索筛选，返回最近11年")
    @GetMapping("/years")
    public Result getYear() {
        int currentYear = LocalDate.now().getYear();
        List<Integer> yearList = IntStream.iterate(currentYear, year -> year - 1)
                .limit(11)
                .boxed()
                .collect(Collectors.toList());
        return Result.success(yearList);
    }
}
