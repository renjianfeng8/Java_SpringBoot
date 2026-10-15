package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.UserMapper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 普通用户账号的注册、登录与密码维护。注册时固定角色为 USER、缺省密码兜底，
 * 密码一律经 BCrypt 存储；更新资料时不允许改动密码与角色字段。
 */
@Service
@Transactional(readOnly = true)
public class UserService extends BaseService<User> {

    private final UserMapper userMapper;

    private final OrderedService orderedService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper, OrderedService orderedService) {
        this.userMapper = userMapper;
        this.orderedService = orderedService;
    }

    @Override
    protected BaseMapper<User> mapper() {
        return userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(User user) {
        insertUser(user);
    }

    private void insertUser(User user) {
        String username = user.getUsername();
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser != null) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT.code(), "账号已存在,请更换别的账号");
        }
        if (user.getPassword() == null) {
            user.setPassword("user123");
        }
        if (user.getName() == null) {
            user.setName(user.getUsername());
        }
        user.setRole(RoleEnum.USER.name());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        mapper().insert(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(User user) {
        // 角色与密码不由此入口改写；置空后交基类——先判存在，再落库
        user.setPassword(null);
        user.setRole(null);
        super.update(user);
    }

    public User login(Account account) {
        String username = account.getUsername();
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "账号不存在");
        }
        String password = account.getPassword();
        if (!passwordEncoder.matches(password, dbUser.getPassword())
                && !password.equals(dbUser.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "账号或密码错误");
        }
        return dbUser;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Account account) {
        Integer id = account.getId();
        User user = selectById(id);
        if (user == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "账号不存在");
        }
        if (!passwordEncoder.matches(account.getPassword(), user.getPassword())
                && !account.getPassword().equals(user.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(account.getNewPassword()));
        userMapper.updatePassword(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public User register(Account account) {
        User user = new User();
        BeanUtils.copyProperties(account, user);
        insertUser(user);
        return user;
    }

    /** 列表：管理员看全部，普通用户只看自己，其他角色拒绝 */
    public List<User> listScoped(User query, String role, Integer userId) {
        if (RoleEnum.ADMIN.name().equals(role)) {
            return selectAll(query);
        }
        if (RoleEnum.USER.name().equals(role)) {
            User user = selectById(userId);
            return user == null ? List.of() : List.of(user);
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
    }

    /** 分页：管理员翻全表，普通用户只回自己一条，其他角色拒绝 */
    public PageInfo<User> pageScoped(User query, String role, Integer userId,
                                     Integer pageNum, Integer pageSize) {
        if (RoleEnum.ADMIN.name().equals(role)) {
            PageMethod.startPage(pageNum, pageSize);
            return new PageInfo<>(selectAll(query));
        }
        if (RoleEnum.USER.name().equals(role)) {
            User user = selectById(userId);
            return new PageInfo<>(user == null ? List.of() : List.of(user));
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
    }

    /** 详情：管理员可查任意用户，普通用户仅可查自己，其他角色拒绝 */
    public User selectByIdScoped(Integer id, String role, Integer userId) {
        if (RoleEnum.ADMIN.name().equals(role)
                || (RoleEnum.USER.name().equals(role) && id.equals(userId))) {
            return selectById(id);
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
    }

    /** 编辑资料：管理员按提交 ID 更新，普通用户强制改自己，其他角色拒绝 */
    @Transactional(rollbackFor = Exception.class)
    public void updateScoped(User user, String role, Integer userId) {
        if (RoleEnum.ADMIN.name().equals(role)) {
            update(user);
            return;
        }
        if (RoleEnum.USER.name().equals(role)) {
            user.setId(userId);
            update(user);
            return;
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
    }

    /** 删除用户：仅管理员，且用户有订单时禁止物理删除 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteScoped(Integer id, String role, Integer userId) {
        requireAdmin(role);
        ensureNoOrders(id);
        delete(id);
    }

    /** 批量删除用户：仅管理员，逐条校验订单引用后再删 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchScoped(List<Integer> ids, String role, Integer userId) {
        requireAdmin(role);
        for (Integer id : ids) {
            ensureNoOrders(id);
        }
        deleteBatch(ids);
    }

    /** 用户有订单时禁止物理删除，避免连带删除交易凭证 */
    private void ensureNoOrders(Integer userId) {
        int orders = orderedService.countByUserId(userId);
        if (orders > 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "该用户已有 " + orders + " 笔订单，无法删除");
        }
    }

    private void requireAdmin(String role) {
        if (!RoleEnum.ADMIN.name().equals(role)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }
}
