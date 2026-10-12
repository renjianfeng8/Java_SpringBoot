package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import org.springframework.stereotype.Service;

/**
 * 登录态的角色分发：按角色把登录/注册/读取当前主体/改密转给 Admin/Cinema/User 三个子服务，
 * 控制器不再自行判断角色分支。各子服务自带事务与校验，本类只做路由。
 */
@Service
public class AuthService {

    private final AdminService adminService;

    private final CinemaService cinemaService;

    private final UserService userService;

    public AuthService(AdminService adminService, CinemaService cinemaService, UserService userService) {
        this.adminService = adminService;
        this.cinemaService = cinemaService;
        this.userService = userService;
    }

    /** 按角色校验账号密码并返回对应主体；角色非法或未取到主体时报参数错误 */
    public Account login(Account account) {
        Account result = null;
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            result = adminService.login(account);
        } else if (RoleEnum.CINEMA.name().equals(account.getRole())) {
            result = cinemaService.login(account);
        } else if (RoleEnum.USER.name().equals(account.getRole())) {
            result = userService.login(account);
        }
        if (result == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "无效的角色类型");
        }
        return result;
    }

    /**
     * 注册：影院注册后待审核（无返回体），普通用户直接返回所建账号；角色非法时报参数错误。
     * 返回 null 表示影院注册路径。
     */
    public User register(Account account) {
        if (RoleEnum.CINEMA.name().equals(account.getRole())) {
            cinemaService.register(account);
            return null;
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            return userService.register(account);
        }
        throw new CustomException(ErrorCode.PARAM_INVALID, "无效的角色类型");
    }

    /** 按角色读取当前登录主体；角色非法或账号不存在时报未授权 */
    public Account me(String role, String userId) {
        if (RoleEnum.ADMIN.name().equals(role)) {
            return requireAccount(adminService.selectById(Integer.valueOf(userId)));
        }
        if (RoleEnum.CINEMA.name().equals(role)) {
            return requireAccount(cinemaService.selectById(Integer.valueOf(userId)));
        }
        if (RoleEnum.USER.name().equals(role)) {
            return requireAccount(userService.selectById(Integer.valueOf(userId)));
        }
        throw new CustomException(ErrorCode.UNAUTHORIZED, "无效的用户角色");
    }

    /** 按角色维护密码；角色非法时报系统错误 */
    public void updatePassword(Account account) {
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            adminService.updatePassword(account);
        } else if (RoleEnum.CINEMA.name().equals(account.getRole())) {
            cinemaService.updatePassword(account);
        } else if (RoleEnum.USER.name().equals(account.getRole())) {
            userService.updatePassword(account);
        } else {
            throw new CustomException(ErrorCode.SYSTEM_ERROR, "非法输入");
        }
    }

    private Account requireAccount(Account account) {
        if (account == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        return account;
    }
}
