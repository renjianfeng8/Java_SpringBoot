package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 评价请求参数，字段与实体 Mark（含关联展示字段）一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkRequest {
    private Integer id;
    private Integer userId;
    private Integer filmId;
    private String img;
    private Double score;
    private String mark;
    private String userName;
    private String filmName;
    private String filmImg;
}
