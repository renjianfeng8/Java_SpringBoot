package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 登录态的角色分发：按角色把登录/注册/读取当前主体/改密转给 Admin/Cinema/User 三个子服务，
 * 控制器不再自行判断角色分支。各子服务自带事务与校验，本类只做路由。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String INVALID_ROLE = "无效的角色类型";

    private final AdminService adminService;

    private final CinemaService cinemaService;

    private final UserService userService;

    private final LoginAttemptService loginAttemptService;

    public AuthService(AdminService adminService, CinemaService cinemaService, UserService userService,
                       LoginAttemptService loginAttemptService) {
        this.adminService = adminService;
        this.cinemaService = cinemaService;
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
    }

    /** 按角色校验账号密码并返回对应主体；角色非法或未取到主体时报参数错误 */
    public Account login(Account account) {
        String role = account.getRole();
        String username = account.getUsername();
        // 计数主体按库中账号 id 归一，拼写变体（重音/全角/大小写）不会各拿一份 5 次。
        // 查库只定位账号、不验密，锁定检查仍在验密之前，且已锁定账号不会被延长锁定期。
        Integer accountId = loginAccountId(role, username);
        loginAttemptService.checkAllowed(role, accountId, username);
        try {
            Account result = null;
            if (RoleEnum.ADMIN.name().equals(role)) {
                result = adminService.login(account);
            } else if (RoleEnum.CINEMA.name().equals(role)) {
                result = cinemaService.login(account);
            } else if (RoleEnum.USER.name().equals(role)) {
                result = userService.login(account);
            }
            if (result == null) {
                throw new CustomException(ErrorCode.PARAM_INVALID, INVALID_ROLE);
            }
            loginAttemptService.onSuccess(role, accountId, username);
            return result;
        } catch (CustomException e) {
            loginAttemptService.onFailure(role, accountId, username);
            throw e;
        }
    }

    /**
     * 登录计数定位：账号存在时返回 id（库的 collation 已把所有拼写变体解析到同一行）；
     * 账号不存在时返回 null，由计数服务退回请求文本。
     */
    private Integer loginAccountId(String role, String username) {
        if (RoleEnum.ADMIN.name().equals(role)) {
            return adminService.findIdByUsername(username);
        }
        if (RoleEnum.CINEMA.name().equals(role)) {
            return cinemaService.findIdByUsername(username);
        }
        if (RoleEnum.USER.name().equals(role)) {
            return userService.findIdByUsername(username);
        }
        return null;
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
        throw new CustomException(ErrorCode.PARAM_INVALID, INVALID_ROLE);
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

    /**
     * 管理员重置他人密码：按目标角色路由到对应账号表。
     *
     * <p>这是唯一不要求原密码的改密入口，权限已在控制器上限定为 ADMIN；
     * 日志只记操作人与目标账号，绝不打印密码。
     */
    public void resetPassword(String role, Integer id, String newPassword, Integer operatorId) {
        if (id == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "账号 ID 不能为空");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 64) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "新密码长度需为 6-64 位");
        }
        log.info("管理员 {} 重置 {} #{} 的登录密码", operatorId, role, id);
        if (RoleEnum.ADMIN.name().equals(role)) {
            adminService.resetPassword(id, newPassword);
        } else if (RoleEnum.CINEMA.name().equals(role)) {
            cinemaService.resetPassword(id, newPassword);
        } else if (RoleEnum.USER.name().equals(role)) {
            userService.resetPassword(id, newPassword);
        } else {
            throw new CustomException(ErrorCode.PARAM_INVALID, INVALID_ROLE);
        }
    }

    private Account requireAccount(Account account) {
        if (account == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        return account;
    }
}
