package com.example.springboot.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**

 订单实体类
 存储电影订单的核心业务信息，包含关联对象的展示字段（用于前端渲染）
 */
@Data // 自动生成 getter、setter、toString、equals、hashCode 方法
@NoArgsConstructor // 生成无参构造方法（满足 Spring 实例化、JSON 反序列化等需求）
@AllArgsConstructor // 生成全参构造方法（便于测试、快速构建完整订单对象）
public class Ordered {
    // 核心业务字段（与数据库订单表字段对应）
    private Integer id; // 订单 ID（主键）
    private String orders; // 订单编号（唯一标识，如：ORD20241001001）
    private Integer userId; // 用户 ID（关联 Account/Admin 等用户表）
    private Integer filmId; // 电影 ID（关联 Film 表）
    private String img; // 订单相关图片（如电影海报、购票凭证）
    private Integer cinemaId; // 影院 ID（关联 Cinema 表）
    private Integer roomId; // 放映厅 ID（关联影院的放映厅表）
    private String appointment; // 预约信息（如：场次预约备注）
    private double total; // 订单总金额（保留两位小数，单位：元）
    private Integer number; // 购票数量（如：2 张）
    private String status; // 订单状态（如：待支付、已完成、已取消）
    private String start; // 电影放映时间（格式：yyyy-MM-dd HH:mm）
    private String seat; // 座位信息（如：A01,A02 多个座位用逗号分隔）
    // 关联查询字段（非数据库存储，通过关联表查询获得，用于前端展示）
    private String userName; // 用户名（通过 userId 关联用户表获取）
    private String filmName; // 电影名称（通过 filmId 关联 Film 表获取）
    private String cinemaName; // 影院名称（通过 cinemaId 关联 Cinema 表获取）
    private String roomName; // 放映厅名称（通过 roomId 关联放映厅表获取）
}