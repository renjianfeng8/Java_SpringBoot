package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.User;
import com.example.springboot.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseController<User> {
    public UserController(UserService userService) {
        super(userService);
    }
}
