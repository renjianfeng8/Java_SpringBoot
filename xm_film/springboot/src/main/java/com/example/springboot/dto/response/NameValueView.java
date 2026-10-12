package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 「维度值 → 计数」的通用分布项：{@code [{name, value}]}。
 * 大盘的影院状态、影片类型、订单状态三张分布卡都复用这一种形状，
 * 后端不派生、不排序、不补零，给什么维度值就是什么，由前端按自己的枚举补全并着色。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameValueView {

    /** 维度值 */
    private String name;

    /** 该维度的计数值（SQL 里的 COUNT(*)，即 BIGINT） */
    private Long value;
}
