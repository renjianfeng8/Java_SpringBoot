package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评价响应视图，字段与实体 Mark 一致。
 * 命名带 Entity 前缀以避开既有的聚合视图 {@link MarkView}（两者字段不同用途不同）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkEntityView {
    private Integer id;
    private Integer userId;
    private Integer filmId;
    private String img;
    private Double score;
    private String comment;
    private String userName;
    private String filmName;
    private String filmImg;
}
