package com.example.springboot.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**

 电影实体类
 包含电影基础信息、关联信息及业务辅助字段
 */
@Data // 自动生成 getter、setter、toString、equals、hashCode 方法
@NoArgsConstructor // 生成无参构造方法
@AllArgsConstructor // 生成包含所有字段的全参构造方法
public class Film {
    // 电影基础信息字段
    private Integer id; // 电影 ID（主键）
    private String title; // 电影中文标题
    private String english; // 电影英文标题
    private String start; // 上映时间（如：2024-01-01）
    private String time; // 电影时长（如：120 分钟）
    private String typeIds; // 电影类型 ID 集合（逗号分隔，如：1,2,3）
    private String language; // 语言（如：中文、英文）
    private String resolution; // 分辨率（如：1080P、4K）
    private String content; // 剧情简介
    private String img; // 封面图片 URL / 路径
    private String employee; // 负责维护的员工（用户名 / ID）
    private Integer areaId; // 所属区域 ID（关联 Area 表）
    private String status; // 状态（如：上映中、已下架、待上映）
    private Double score; // 评分（如：9.2）
    private String typeId; // 单个类型 ID（简化查询用，与 typeIds 对应）
    private String year; // 发行年份（如：2024）
    // 关联查询结果字段（非数据库存储，用于前端展示）
    private String areaName; // 所属区域名称（通过 areaId 关联查询获得）
    // 业务辅助字段（用于参数传递 / 批量操作）
    private List<Integer> ids; // ID 集合（如批量删除 / 查询时使用）
    private List<String> types; // 类型名称集合（通过 typeIds 关联查询获得）

    // 新增票房相关字段
    private double boxOffice; // 票房数据
    private Integer actorId;
    private String actorInfo;
    // 排行榜查询辅助字段
    private Integer topNum; // 排行榜显示数量（如：10表示Top10）
    private String video;
}