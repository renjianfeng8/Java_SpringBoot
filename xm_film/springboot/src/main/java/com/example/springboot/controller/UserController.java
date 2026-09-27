package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.OrderedService;
import com.example.springboot.service.UserService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Tag(name = "用户管理", description = "普通用户 CRUD")
@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseController<User> {
    private final UserService userService;
    private final OrderedService orderedService;

    public UserController(UserService userService, OrderedService orderedService) {
        super(userService);
        this.userService = userService;
        this.orderedService = orderedService;
    }

    @Override
    @GetMapping
    public Result list(User entity) {
        if (isAdmin()) {
            return Result.success(userService.selectAll(entity));
        }
        if (isUser()) {
            User user = userService.selectById(currentUserId());
            return Result.success(user == null ? List.of() : List.of(user));
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
    }

    @Override
    @GetMapping("/page")
    public Result page(User entity,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        if (isAdmin()) {
            PageMethod.startPage(pageNum, pageSize);
            return Result.success(new PageInfo<>(userService.selectAll(entity)));
        }
        if (isUser()) {
            User user = userService.selectById(currentUserId());
            return Result.success(new PageInfo<>(user == null ? List.of() : List.of(user)));
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
    }

    @Override
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        if (isAdmin() || (isUser() && id.equals(currentUserId()))) {
            return Result.success(userService.selectById(id));
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
    }

    @Override
    @PutMapping
    public Result update(@RequestBody User entity) {
        if (isAdmin()) {
            userService.update(entity);
            return Result.success();
        }
        if (isUser()) {
            entity.setId(currentUserId());
            userService.update(entity);
            return Result.success();
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
    }

    @Override
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        requireAdmin();
        ensureNoOrders(id);
        userService.delete(id);
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        requireAdmin();
        for (Integer id : ids) {
            ensureNoOrders(id);
        }
        userService.deleteBatch(ids);
        return Result.success();
    }

    /** 用户有订单时禁止物理删除，避免连带删除交易凭证 */
    private void ensureNoOrders(Integer userId) {
        int orders = orderedService.countByUserId(userId);
        if (orders > 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "该用户已有 " + orders + " 笔订单，无法删除");
        }
    }

    private boolean isAdmin() {
        return "ADMIN".equals(currentRole());
    }

    private boolean isUser() {
        return "USER".equals(currentRole());
    }

    private void requireAdmin() {
        if (!isAdmin()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }

    private String currentRole() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : (String) request.getAttribute("role");
    }

    private Integer currentUserId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String userId = (String) request.getAttribute("userId");
        return userId == null ? null : Integer.valueOf(userId);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
