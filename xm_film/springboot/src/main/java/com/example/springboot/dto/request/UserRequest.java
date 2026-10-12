package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 用户请求参数，字段与实体 User（含 Account 继承字段）一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    private Integer id;
    private String username;
    private String password;
    private String role;
    private String name;
    private String newPassword;
    private String token;
    private String avatar;
    private String phone;
    private String email;
}
