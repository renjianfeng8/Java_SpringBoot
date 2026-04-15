package com.example.springboot.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**

 电影放映记录实体类
 存储影院放映电影的核心信息，包含关联对象的展示字段
 */
@Data // 自动生成 getter、setter、toString、equals、hashCode 方法
@NoArgsConstructor // 生成无参构造方法（满足 Spring 实例化、JSON 反序列化等需求）
@AllArgsConstructor // 生成包含所有字段的全参构造方法（便于测试、快速构建对象）
public class Record {
    // 核心业务字段（与数据库表字段对应）
    private Integer id;
    private Integer cinemaId; // 影院 ID（改为 Integer，与数据库 int 类型匹配，关联 Cinema 表）
    private Integer roomId; // 放映厅 ID（改为 Integer，与数据库 int 类型匹配，关联影院放映厅表）
    private String title; // 电影名称
    private String start; // 放映时间（格式通常为：yyyy-MM-dd HH:mm）
    private String price; // 票价（用 String 类型适配多样式展示，如："35.00 元"、"特惠 29 元"）
    private String status; // 放映状态（如："未开始"、"放映中"、"已结束"）
    // 关联查询字段（非数据库存储，通过关联表查询获得，用于前端展示）
    private String cinemaName; // 影院名称（通过 cinemaId 关联 Cinema 表获取）
    private String roomName; // 放映厅名称（通过 roomId 关联放映厅表获取）
}