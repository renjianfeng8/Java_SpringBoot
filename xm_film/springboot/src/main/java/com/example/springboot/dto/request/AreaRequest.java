package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 区域请求参数，字段与实体 Area 一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaRequest {
    private Integer id;
    private String title;
}
