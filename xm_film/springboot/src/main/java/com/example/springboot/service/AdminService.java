package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Admin;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.AdminMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT.code(), "账号已存在,请更换别的账号");
        }
        if (admin.getPassword() == null) {
            admin.setPassword("admin123");
        }
        if (admin.getName() == null) {
            admin.setName(admin.getUsername());
        }
        admin.setRole(RoleEnum.ADMIN.name());
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        mapper().insert(admin);
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
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "账号不存在");
        }
        String password = account.getPassword();
        if (!passwordEncoder.matches(password, dbAdmin.getPassword())
                && !password.equals(dbAdmin.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "账号或密码错误");
        }
        return dbAdmin;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Account account) {
        Integer id = account.getId();
        Admin admin = selectById(id);
        if (admin == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "账号不存在");
        }
        if (!passwordEncoder.matches(account.getPassword(), admin.getPassword())
                && !account.getPassword().equals(admin.getPassword())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.code(), "原密码错误");
        }
        admin.setPassword(passwordEncoder.encode(account.getNewPassword()));
        adminMapper.updatePassword(admin);
    }
}
