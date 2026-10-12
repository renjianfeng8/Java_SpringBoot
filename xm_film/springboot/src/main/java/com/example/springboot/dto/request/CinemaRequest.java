package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 影院请求参数，字段与实体 Cinema（含 Account 继承字段与 JOIN 展示字段）一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaRequest {
    private Integer id;
    private String username;
    private String password;
    private String role;
    private String name;
    private String newPassword;
    private String token;
    private String avatar;
    private String email;
    private String address;
    private String leader;
    private String code;
    private String certificate;
    private String status;
    private String phone;
    private String description;
    private Integer directoryId;
    private String province;
    private String city;
    private String district;
}
