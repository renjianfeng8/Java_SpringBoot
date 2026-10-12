package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 类型响应视图，字段与实体 Type 一致。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypeView {
    private Integer id;
    private String title;
}
