package com.example.springboot.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 评价点赞关系实体（一行 = 某用户对某条评价点的一个赞，唯一键 (markId, userId) 即"一人一赞"） */
@Data // 自动生成 getter、setter、toString、equals、hashCode 方法
@NoArgsConstructor // 生成无参构造方法
@AllArgsConstructor // 生成包含所有字段的全参构造方法
public class MarkLike {
    private Integer id; // 主键 ID
    private Integer markId; // 被点赞的评价 ID（关联 mark 表）
    private Integer userId; // 点赞人 ID（关联 user 表）
}
