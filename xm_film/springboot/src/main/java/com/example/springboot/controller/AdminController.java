package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.dto.request.AdminRequest;
import com.example.springboot.dto.response.AdminView;
import com.example.springboot.entity.Admin;
import com.example.springboot.service.AdminService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admins")
public class AdminController extends BaseController<Admin, AdminRequest, AdminView> {
    public AdminController(AdminService adminService) {
        super(adminService);
    }

    @Override
    protected Admin toEntity(AdminRequest request) {
        Admin admin = new Admin();
        admin.setId(request.getId());
        admin.setUsername(request.getUsername());
        admin.setPassword(request.getPassword());
        admin.setRole(request.getRole());
        admin.setName(request.getName());
        admin.setNewPassword(request.getNewPassword());
        admin.setToken(request.getToken());
        admin.setAvatar(request.getAvatar());
        admin.setPhone(request.getPhone());
        return admin;
    }

    @Override
    protected AdminView toView(Admin admin) {
        AdminView view = new AdminView();
        view.setId(admin.getId());
        view.setUsername(admin.getUsername());
        view.setRole(admin.getRole());
        view.setName(admin.getName());
        view.setAvatar(admin.getAvatar());
        view.setPhone(admin.getPhone());
        return view;
    }
}
