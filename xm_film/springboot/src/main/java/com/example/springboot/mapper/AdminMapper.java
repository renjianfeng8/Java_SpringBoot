package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Admin;

public interface AdminMapper extends BaseMapper<Admin> {

    Admin selectByUsername(String username);

}
