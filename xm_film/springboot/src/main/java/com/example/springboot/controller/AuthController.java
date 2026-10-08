package com.example.springboot.controller;

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
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

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

    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterRequest request) {
        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setPassword(request.getPassword());
        account.setRole(request.getRole());
        if (RoleEnum.CINEMA.name().equals(account.getRole())) {
            cinemaService.register(account);
        } else if (RoleEnum.USER.name().equals(account.getRole())) {
            com.example.springboot.entity.User user = userService.register(account);
            return Result.success(Map.of("id", user.getId()));
        } else {
            throw new CustomException(ErrorCode.PARAM_INVALID, "无效的角色类型");
        }
        return Result.success();
    }

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

    @GetMapping("/me")
    public Result me(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        Account account = null;
        if (RoleEnum.ADMIN.name().equals(role)) {
            account = adminService.selectById(Integer.valueOf(userId));
        } else if (RoleEnum.CINEMA.name().equals(role)) {
            account = cinemaService.selectById(Integer.valueOf(userId));
        } else if (RoleEnum.USER.name().equals(role)) {
            account = userService.selectById(Integer.valueOf(userId));
        } else {
            return Result.error(ErrorCode.UNAUTHORIZED.code(), "无效的用户角色");
        }
        if (account == null) {
            return Result.error(ErrorCode.UNAUTHORIZED.code(), "账号不存在");
        }
        account.setPassword(null);
        return Result.success(account);
    }

    @GetMapping("/years")
    public Result getYear() {
        int currentYear = LocalDate.now(ZoneId.systemDefault()).getYear();
        List<Integer> yearList = IntStream.iterate(currentYear, year -> year - 1)
                .limit(11)
                .boxed()
                .toList();
        return Result.success(yearList);
    }
}
