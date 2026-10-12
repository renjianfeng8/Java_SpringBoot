package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录 / 当前用户响应视图：Account（roles 通用字段）与 User / Admin / Cinema 三类主体字段的并集。
 * 剔除 password、newPassword、token —— password 原为 WRITE_ONLY 不入 JSON，token 由控制器另行注入。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountView {
    private Integer id;
    private String username;
    private String role;
    private String name;
    private String token;
    private String avatar;
    private String phone;
    private String email;
    private String address;
    private String leader;
    private String code;
    private String certificate;
    private String status;
    private String description;
    private Integer directoryId;
    private String province;
    private String city;
    private String district;
}
