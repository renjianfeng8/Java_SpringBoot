package com.example.springboot.entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**

 评分 / 标记实体类
 用于存储用户对电影的评分、标记相关信息，包含关联对象的展示字段
 */
@Getter // 生成 getter 方法
@Setter // 生成 setter 方法
@NoArgsConstructor // 生成无参构造方法
@AllArgsConstructor // 生成包含所有字段的全参构造方法
public class Mark implements Serializable {
    private static final long serialVersionUID = 1L;

    // 核心业务字段（与数据库表字段对应）
    private Integer id; // 主键 ID
    private Integer userId; // 用户 ID（关联 Account/Admin/Cinema 等用户表）
    private Integer filmId; // 电影 ID（关联 Film 表）
    private String img; // 相关图片（如评分截图、标记配图）
    private Double score; // 评分（0.0~10.0）—— 影片评分的唯一数值来源，film.score 由它聚合回写
    private String comment; // 评语（DB 列 comment，原列名 mark 与表名同名、易生歧义，已改名）
    // 关联展示字段（非数据库存储，通过关联查询获取，用于前端展示）
    private String userName; // 用户名（通过 userId 关联查询用户表获得）
    private String filmName; // 电影名（通过 filmId 关联查询电影表获得）
    private String filmImg; // 电影海报（通过 filmId 关联查询电影表获得）

}