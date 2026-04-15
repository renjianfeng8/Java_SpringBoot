package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends Account {
    private Integer id;
    private String username;
    private String password;
    private String role;
    private String name;
    private String avatar;
    private String phone;
    private String email;
}
