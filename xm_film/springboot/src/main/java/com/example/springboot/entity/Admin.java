package com.example.springboot.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 管理员实体类
 * 继承自Account类，使用Lombok注解简化代码
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Admin extends Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private String avatar;
    private String phone;
}
