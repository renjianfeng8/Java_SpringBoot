package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 类型请求参数，字段与实体 Type 一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypeRequest {
    private Integer id;
    private String title;
}
