package com.example.springboot.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**

 影院放映厅实体类
 存储影院内放映厅的基础信息
 */
@Data // 自动生成 getter、setter、toString、equals、hashCode 方法
@NoArgsConstructor // 生成无参构造方法（满足 Spring 实例化、JSON 反序列化等需求）
@AllArgsConstructor // 生成包含所有字段的全参构造方法（便于测试、快速构建对象）
public class Room {
    private Integer id; // 放映厅 ID（主键，唯一标识）
    private String title; // 放映厅标题（如："IMAX 厅"，用于突出特色）
    private String name; // 放映厅名称（如："1 号厅"，用于基础标识）
}