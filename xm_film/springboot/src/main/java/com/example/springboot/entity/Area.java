package com.example.springboot.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 区域实体类
 * 使用Lombok注解简化代码
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Area implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String title;
}
