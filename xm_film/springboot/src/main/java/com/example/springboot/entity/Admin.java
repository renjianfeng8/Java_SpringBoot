package com.example.springboot.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 管理员实体类
 * 继承自Account类，使用Lombok注解简化代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Admin extends Account {
    private String avatar;
    private String phone;
    private String email;
}
