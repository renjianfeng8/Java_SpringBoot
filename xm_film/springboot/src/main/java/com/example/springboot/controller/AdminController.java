package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Admin;
import com.example.springboot.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员管理", description = "系统管理员 CRUD")
@RestController
@RequestMapping("/api/v1/admins")
public class AdminController extends BaseController<Admin> {
    public AdminController(AdminService adminService) {
        super(adminService);
    }
}
