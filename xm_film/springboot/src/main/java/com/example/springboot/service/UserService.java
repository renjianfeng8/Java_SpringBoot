package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.common.BatchDeleteGuard;
import com.example.springboot.common.PasswordMatcher;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.UserMapper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    private final PasswordEncoder passwordEncoder = PasswordMatcher.instance();

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
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "账号已存在,请更换别的账号");
        }
        if (user.getPassword() == null) {
            user.setPassword("user123");
        }
        if (user.getName() == null) {
            user.setName(user.getUsername());
        }
        user.setRole(RoleEnum.USER.name());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        try {
            mapper().insert(user);
        } catch (DuplicateKeyException e) {
            // 上面的 selectByUsername 是"先查后插"：并发注册同名时两个请求都可能读到 null。
            // username 上的唯一索引是最终防线，撞键时这里必须给出与串行路径**同一句**
            // 提示，而不是让 DuplicateKeyException 漏到全局处理器变成 500。
            // 本地单实例下 MySQL 的 INSERT 会阻塞同名并发插入，该窗口难以通过 HTTP
            // 复现（实测 8 线程 × 5 轮均为 1 成功 + 7 个 409），但守护仍不可省：
            // 多实例部署或重试场景下它就是唯一的正确性来源。
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "账号已存在,请更换别的账号");
        }
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
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        String password = account.getPassword();
        if (!PasswordMatcher.matches(passwordEncoder, password, dbUser.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        return dbUser;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Account account) {
        Integer id = account.getId();
        User user = selectById(id);
        if (user == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        if (!PasswordMatcher.matches(passwordEncoder, account.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(account.getNewPassword()));
        userMapper.updatePassword(user);
    }

    /** 管理员重置登录密码：不校验原密码，权限由调用方限定为 ADMIN */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Integer id, String newPassword) {
        if (selectById(id) == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        User patch = new User();
        patch.setId(id);
        patch.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updatePassword(patch);
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
        if (ids == null || ids.isEmpty()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择用户");
        }
        requireAdmin(role);
        List<String> blocked = new ArrayList<>();
        for (Integer id : ids) {
            try {
                ensureNoOrders(id);
            } catch (CustomException e) {
                if (!BatchDeleteGuard.isBlocked(e)) {
                    throw e;
                }
                blocked.add("用户 #" + id + "：" + e.getMsg());
            }
        }
        BatchDeleteGuard.throwIfBlocked(blocked);
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
