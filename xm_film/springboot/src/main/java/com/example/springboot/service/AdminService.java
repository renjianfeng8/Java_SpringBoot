package com.example.springboot.service;

import com.example.springboot.common.FileUtil;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Admin;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.AdminMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service//标记为服务层组件
@Transactional(rollbackFor = Exception.class)
public class AdminService {

    @Resource
    private AdminMapper adminMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<Admin> selectAll(Admin admin) {
        return adminMapper.selectAll(admin);
    }

    public Admin selectById(Integer id) {
        return adminMapper.selectById(id);

    }

    public List<Admin> selectList(Admin admin) {
        return adminMapper.selectAll(admin);
    }

    public PageInfo<Admin> selectPage( Admin admin, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Admin> list = adminMapper.selectAll(admin);
        return PageInfo.of(list);
    }

    public void add(Admin admin) {
        String username = admin.getUsername(); //账号
        Admin dbAdmin = adminMapper.selectByUsername(username);
        if (dbAdmin != null) { //账号已存在
            throw new CustomException("500","账号已存在,请更换别的账号");
        }
        if (admin.getPassword() == null) { //密码没填
            admin.setPassword("admin123"); //默认密码admin123
        }
        if (admin.getName() == null) { //名字
            admin.setName(admin.getUsername());  //默认名称
        }
        //设置角色
        admin.setRole("ADMIN"); //管理员角色
        admin.setPassword(passwordEncoder.encode(admin.getPassword())); // BCrypt加密密码
        adminMapper.insert(admin);
    }

    public void update(Admin admin) {
        admin.setPassword(null);
        admin.setRole(null);
        adminMapper.updateById(admin);
    }

    public void delete(Integer id) {
        adminMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.delete(id);
        }
    }

    public Admin login(Account account) {
        String username = account.getUsername(); //账号
        Admin dbAdmin = adminMapper.selectByUsername(username);
        if (dbAdmin == null) { //没查询到账户
            throw new CustomException("500","账号不存在");
        }
        //数据库存在这个账号
        String password = account.getPassword();
        if (!passwordEncoder.matches(password, dbAdmin.getPassword())) {
            // 兼容 data.sql 明文初始密码
            if (!dbAdmin.getPassword().equals(password)) {
                throw new CustomException("500","账号或密码错误");
            }
        }
        return dbAdmin;
    }

    public void updatePassword(Account account) {
        Integer id = account.getId();
        Admin admin = this.selectById(id);
        if (admin == null) {
            throw new CustomException("500", "账号不存在");
        }
        if (!passwordEncoder.matches(account.getPassword(), admin.getPassword())) {
            // 兼容 data.sql 明文初始密码
            if (!admin.getPassword().equals(account.getPassword())) {
                throw new CustomException("500","原密码错误");
            }
        }
        admin.setPassword(passwordEncoder.encode(account.getNewPassword()));  //设置加密后的新密码
        this.update(admin);
    }

    public String uploadFile(MultipartFile file, String uploadDir) throws IOException {
        return FileUtil.uploadFile(file, uploadDir);
    }
}
