package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 区域响应视图，字段与实体 Area 一致。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaView {
    private Integer id;
    private String title;
}
