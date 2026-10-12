package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 管理员响应视图，剔除 password、newPassword、token。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminView {
    private Integer id;
    private String username;
    private String role;
    private String name;
    private String avatar;
    private String phone;
    private String email;
}
