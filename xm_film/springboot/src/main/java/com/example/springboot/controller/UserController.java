package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.request.UserRequest;
import com.example.springboot.dto.response.UserView;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.UserService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseController<User, UserRequest, UserView> {
    private final UserService userService;

    public UserController(UserService userService) {
        super(userService);
        this.userService = userService;
    }

    @Override
    protected User toEntity(UserRequest request) {
        User user = new User();
        user.setId(request.getId());
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        user.setName(request.getName());
        user.setNewPassword(request.getNewPassword());
        user.setToken(request.getToken());
        user.setAvatar(request.getAvatar());
        user.setPhone(request.getPhone());
        return user;
    }

    @Override
    protected UserView toView(User user) {
        UserView view = new UserView();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setRole(user.getRole());
        view.setName(user.getName());
        view.setAvatar(user.getAvatar());
        view.setPhone(user.getPhone());
        return view;
    }

    /** 新增用户仅管理员：/api/v1/users 前缀不在 ADMIN_WRITE_PREFIXES（普通用户要能改自己的资料），
     *  故新增必须在此显式守卫，否则任何登录角色都能建号 */
    @Override
    @PostMapping
    public Result<Void> add(@RequestBody UserRequest request) {
        requireAdmin();
        userService.add(toEntity(request));
        return Result.success();
    }

    @Override
    @GetMapping
    public Result<List<UserView>> list(UserRequest query) {
        return Result.success(userService.listScoped(toEntity(query), AuthContext.role(), AuthContext.userId())
                .stream().map(this::toView).toList());
    }

    @Override
    @GetMapping("/page")
    public Result<PageInfo<UserView>> page(UserRequest query,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(toPageView(userService.pageScoped(toEntity(query), AuthContext.role(),
                AuthContext.userId(), pageNum, pageSize)));
    }

    private void requireAdmin() {
        if (!RoleEnum.ADMIN.name().equals(AuthContext.role())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }

    @Override
    @GetMapping("/{id}")
    public Result<UserView> getById(@PathVariable Integer id) {
        User user = userService.selectByIdScoped(id, AuthContext.role(), AuthContext.userId());
        return Result.success(user == null ? null : toView(user));
    }

    @Override
    @PutMapping
    public Result<Void> update(@RequestBody UserRequest request) {
        userService.updateScoped(toEntity(request), AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @Override
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        userService.deleteScoped(id, AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        userService.deleteBatchScoped(ids, AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

}
