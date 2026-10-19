package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.common.PasswordMatcher;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Admin;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.AdminMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员账号的登录与密码维护。管理员只由后台创建，没有自助注册入口；
 * 密码一律经 BCrypt 存储，更新资料时不允许改动密码与角色字段。
 */
@Service
@Transactional(readOnly = true)
public class AdminService extends BaseService<Admin> {

    private final AdminMapper adminMapper;

    private final PasswordEncoder passwordEncoder = PasswordMatcher.instance();

    public AdminService(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @Override
    protected BaseMapper<Admin> mapper() {
        return adminMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Admin admin) {
        String username = admin.getUsername();
        Admin dbAdmin = adminMapper.selectByUsername(username);
        if (dbAdmin != null) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "账号已存在,请更换别的账号");
        }
        if (admin.getPassword() == null) {
            admin.setPassword("admin123");
        }
        if (admin.getName() == null) {
            admin.setName(admin.getUsername());
        }
        admin.setRole(RoleEnum.ADMIN.name());
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        try {
            mapper().insert(admin);
        } catch (DuplicateKeyException e) {
            // 先查后插的窗口：并发注册同名时两个请求都可能读到 null，username 唯一索引
            // 是最终防线。撞键必须给出与串行路径同一句提示，而不是漏成 500。
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "账号已存在,请更换别的账号");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Admin admin) {
        admin.setPassword(null);
        admin.setRole(null);
        super.update(admin);
    }

    public Admin login(Account account) {
        String username = account.getUsername();
        Admin dbAdmin = adminMapper.selectByUsername(username);
        if (dbAdmin == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        String password = account.getPassword();
        if (!PasswordMatcher.matches(passwordEncoder, password, dbAdmin.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        return dbAdmin;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Account account) {
        Integer id = account.getId();
        Admin admin = selectById(id);
        if (admin == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        if (!PasswordMatcher.matches(passwordEncoder, account.getPassword(), admin.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "原密码错误");
        }
        admin.setPassword(passwordEncoder.encode(account.getNewPassword()));
        adminMapper.updatePassword(admin);
    }
}
