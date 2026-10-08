package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Admin;
import com.example.springboot.service.AdminService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admins")
public class AdminController extends BaseController<Admin> {
    public AdminController(AdminService adminService) {
        super(adminService);
    }
}
