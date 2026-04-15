package com.example.springboot.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 演员/角色实体类
 * 使用Lombok注解简化代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Actor {
    private Integer id;
    private String title;
    private String img;
    private String actor;
    private String figure;
    private String picture;
    private String grade;
    private String video;
}
