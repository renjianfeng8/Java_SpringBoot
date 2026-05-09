package com.example.springboot.service;

import com.example.springboot.common.FileUtil;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.UserMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<User> selectAll(User user) {
        return userMapper.selectAll(user);
    }

    public User selectById(Integer id) {
        return userMapper.selectById(id);

    }

    public List<User> selectList(User user) {
        System.out.println(user);
        return null;
    }

    public PageInfo<User> selectPage( User user, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> list = userMapper.selectAll(user);
        return PageInfo.of(list);
    }

    public void add(User user) {
        String username = user.getUsername(); //账号
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser != null) { //账号已存在
            throw new CustomException("500","账号已存在,请更换别的账号");
        }
        if (user.getPassword() == null) { //密码没填
            user.setPassword("user123"); //默认密码user123
        }
        if (user.getName() == null) { //名字
            user.setName(user.getUsername());  //默认名称
        }
        //设置角色
        user.setRole("USER"); //用户角色
        user.setPassword(passwordEncoder.encode(user.getPassword())); // BCrypt加密密码
        userMapper.insert(user);
    }

    public void update(User user) {
        if (user == null) {
            throw new IllegalArgumentException("更新用户时，user对象不能为空");
        }
        userMapper.updateById(user);
    }

    public void delete(Integer id) {
        userMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.delete(id);
        }
    }

    public User login(Account account) {
        String username = account.getUsername(); //账号
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser == null) { //没查询到账户
            throw new CustomException("500","账号不存在");
        }
        //数据库存在这个账号
        String password = account.getPassword();
        if (!passwordEncoder.matches(password, dbUser.getPassword())) {
            // 兼容旧版明文密码（迁移过渡）
            if (!dbUser.getPassword().equals(password)) {
                throw new CustomException("500","账号或密码错误");
            }
        }
        return dbUser;
    }

    public void updatePassword(Account account) {
        Integer id = account.getId();
        User user = this.selectById(id);
        if (!passwordEncoder.matches(account.getPassword(), user.getPassword())) {
            // 兼容旧版明文密码
            if (!user.getPassword().equals(account.getPassword())) {
                throw new CustomException("500","原密码错误");
            }
        }
        user.setPassword(passwordEncoder.encode(account.getNewPassword()));  //设置加密后的新密码
        this.update(user);
    }

    public String uploadFile(MultipartFile file, String uploadDir) throws IOException {
        return FileUtil.uploadFile(file, uploadDir);
    }

    public void register(Account account) {
        User user = new User();
        BeanUtils.copyProperties(account,user);
        add(user);
    }
}
