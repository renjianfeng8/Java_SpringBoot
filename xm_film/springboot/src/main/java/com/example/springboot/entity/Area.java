package com.example.springboot.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 区域实体类
 * 使用Lombok注解简化代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Area {
    private Integer id;
    private String title;
}
