package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.User;
import com.example.springboot.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理", description = "普通用户 CRUD")
@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseController<User> {
    public UserController(UserService userService) {
        super(userService);
    }
}
