package com.example.springboot.mapper;

import com.example.springboot.entity.User;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper {

    List<User> selectAll(User user);

    @Select("select * from user where id = #{id}")
    User selectById(Integer id);

    void insert(User user);

    void updateById(User user);

    void deleteById(Integer id);

    User selectByUsername(String username);


}
