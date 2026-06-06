package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService extends BaseService<User> {

    @Resource
    private UserMapper userMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    protected BaseMapper<User> mapper() {
        return userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(User user) {
        String username = user.getUsername();
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser != null) {
            throw new CustomException("500", "账号已存在,请更换别的账号");
        }
        if (user.getPassword() == null) {
            user.setPassword("user123");
        }
        if (user.getName() == null) {
            user.setName(user.getUsername());
        }
        user.setRole("USER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        mapper().insert(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(User user) {
        user.setPassword(null);
        user.setRole(null);
        mapper().updateById(user);
    }

    public User login(Account account) {
        String username = account.getUsername();
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser == null) {
            throw new CustomException("500", "账号不存在");
        }
        String password = account.getPassword();
        if (!passwordEncoder.matches(password, dbUser.getPassword())) {
            if (!dbUser.getPassword().equals(password)) {
                throw new CustomException("500", "账号或密码错误");
            }
        }
        return dbUser;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Account account) {
        Integer id = account.getId();
        User user = selectById(id);
        if (user == null) {
            throw new CustomException("500", "账号不存在");
        }
        if (!passwordEncoder.matches(account.getPassword(), user.getPassword())) {
            if (!user.getPassword().equals(account.getPassword())) {
                throw new CustomException("500", "原密码错误");
            }
        }
        user.setPassword(passwordEncoder.encode(account.getNewPassword()));
        userMapper.updatePassword(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(Account account) {
        User user = new User();
        BeanUtils.copyProperties(account, user);
        add(user);
    }
}
