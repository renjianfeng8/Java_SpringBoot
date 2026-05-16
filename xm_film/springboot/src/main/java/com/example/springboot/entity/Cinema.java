package com.example.springboot.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * 影院实体类
 * 继承自Account类，使用Lombok注解简化代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true) // 调用父类的equals和hashCode方法
public class Cinema extends Account {
    private Integer id;
    private String username;
    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;
    private String role;
    private String name;
    private String avatar;
    private String email;
    private String address;
    private String leader;
    private String code;
    private String certificate;
    private String status;
    private String phone;
    private String description;
}
