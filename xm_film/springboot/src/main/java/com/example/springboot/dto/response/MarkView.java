package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一条评价在访问者眼中的样子。
 *
 * 刻意**不带 userId**：发送作者的 id 等于把"这条是不是我写的"下放给前端自己比对，
 * 这正是当初那处漏（/api/v1/orders/seats 曾直接下发他人订单号与 userId）。
 * 归属由后端算好，以 {@code mine} 布尔量下发；同理 liked 也是后端按访问者算好的。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkView {

    private Integer id;

    /** 评价人昵称（LEFT JOIN user.name） */
    private String userName;

    /** 评价人头像（LEFT JOIN user.avatar） */
    private String avatar;

    private Double score;

    private String mark;

    /** 该条评价的赞数，由 mark_like 关系表实时聚合（无计数器列） */
    private Integer likeCount;

    /** 当前访问者是否赞过（匿名恒 false） */
    private Boolean liked;

    /** 是否本人所写（后端算，匿名恒 false） */
    private Boolean mine;
}
