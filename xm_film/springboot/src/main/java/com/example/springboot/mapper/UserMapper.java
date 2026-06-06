package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.User;

public interface UserMapper extends BaseMapper<User> {

    User selectByUsername(String username);

    void updatePassword(User user);

}
