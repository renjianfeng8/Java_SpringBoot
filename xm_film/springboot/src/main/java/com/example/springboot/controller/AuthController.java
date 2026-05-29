package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.Account;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.CinemaService;
import com.example.springboot.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
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
    public Result login(@RequestBody Account account) {
        Account result = null;
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            result = adminService.login(account);
        } else if (RoleEnum.CINEMA.name().equals(account.getRole())) {
            result = cinemaService.login(account);
        } else if (RoleEnum.USER.name().equals(account.getRole())) {
            result = userService.login(account);
        }
        if (result == null) {
            return Result.error("500", "账号或密码错误");
        }
        String token = jwtUtils.generateToken(result.getId(), result.getRole());
        result.setToken(token);
        return Result.success(result);
    }

    @PostMapping("/register")
    public Result register(@RequestBody Account account) {
        if (RoleEnum.CINEMA.name().equals(account.getRole())) {
            cinemaService.register(account);
        } else if (RoleEnum.USER.name().equals(account.getRole())) {
            userService.register(account);
        } else {
            throw new CustomException("400", "无效的角色类型");
        }
        return Result.success();
    }

    @PutMapping("/password")
    public Result updatePassword(@RequestBody Account account, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId != null) {
            account.setId(Integer.valueOf(userId));
        }
        if ("ADMIN".equals(account.getRole())) {
            adminService.updatePassword(account);
        } else if ("CINEMA".equals(account.getRole())) {
            cinemaService.updatePassword(account);
        } else if ("USER".equals(account.getRole())) {
            userService.updatePassword(account);
        } else {
            throw new CustomException("500", "非法输入");
        }
        return Result.success();
    }

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
