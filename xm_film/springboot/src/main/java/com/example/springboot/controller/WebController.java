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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 系统Web接口控制器
 * 负责处理用户登录、注册和密码修改等通用功能
 */
@RestController
public class WebController {


    @Resource
    private AdminService adminService;

    @Resource
    private CinemaService  cinemaService;

    @Resource
    private UserService userService;

    @Resource
    private JwtUtils jwtUtils;

    /**
     * 登录接口
     * 根据账户角色调用不同的服务实现登录验证
     * @param account 包含用户名、密码和角色的账户对象
     * @return 返回登录成功的账户信息
     * @throws CustomException 当角色非法时抛出异常
     */
    @PostMapping("/login")
    public Result login(@RequestBody Account account){
        Account result = null;
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            result = adminService.login(account);
        }
        if (RoleEnum.CINEMA.name().equals(account.getRole())) {
            result = cinemaService.login(account);
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
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
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            userService.register(account);
        }
        return Result.success();
    }


    /**
     * 修改密码接口
     * 根据账户角色(ADMIN/EMP)调用不同的服务实现密码修改
     * @param account 包含用户名、角色和新密码的账户对象
     * @return 返回操作结果
     * @throws CustomException 当角色非法时抛出异常
     */
    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account){
        if ("ADMIN".equals(account.getRole())) {
            adminService.updatePassword(account);
        } else if ("CINEMA".equals(account.getRole())) {
            cinemaService.updatePassword(account);
        } else if ("USER".equals(account.getRole())) {
            userService.updatePassword(account);
        } else {
            throw new CustomException("500","非法输入");
        }
        return Result.success();
    }

    @GetMapping("/getYear")
    public Result getYear() {
        int currentYear = LocalDate.now().getYear();

        // 从当前年份开始递减，生成11个年份（2025, 2024, ..., 2015）
        List<Integer> yearList = IntStream.iterate(currentYear, year -> year - 1)
                .limit(11)
                .boxed()
                .collect(Collectors.toList());

        return Result.success(yearList);
    }

}