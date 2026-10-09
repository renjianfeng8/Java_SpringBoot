package com.example.springboot.common;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 影院名称 → 连锁品牌。高德不返回「所属院线」，只能按名称关键词派生。
 * 规则有序：长/具体词在前（如「橙天嘉禾」先于「嘉禾」），首个命中即返回。
 * 未命中返回 null（前端显示为「独立/其他」），词表可按需增补。
 */
public final class CinemaBrand {

    private static final List<Map.Entry<String, String>> RULES = List.of(
            Map.entry("万达", "万达影城"),
            Map.entry("CGV", "CGV影城"),
            Map.entry("横店", "横店影视"),
            Map.entry("大地", "大地影院"),
            Map.entry("金逸", "金逸影城"),
            Map.entry("UME", "UME影城"),
            Map.entry("博纳", "博纳国际影城"),
            Map.entry("中影", "中影国际影城"),
            Map.entry("幸福蓝海", "幸福蓝海国际影城"),
            Map.entry("保利", "保利国际影城"),
            Map.entry("卢米埃", "卢米埃影城"),
            Map.entry("橙天嘉禾", "橙天嘉禾"),
            Map.entry("嘉禾", "橙天嘉禾"),
            Map.entry("百丽宫", "百丽宫影城"),
            Map.entry("英皇", "英皇电影城"),
            Map.entry("太平洋", "太平洋影城"),
            Map.entry("苏宁", "苏宁影城"),
            Map.entry("SFC", "SFC上影影城"),
            Map.entry("上影", "SFC上影影城"),
            Map.entry("星轶", "星轶影城")
    );

    private CinemaBrand() {
    }

    public static String derive(String name) {
        if (name == null) {
            return null;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        for (Map.Entry<String, String> rule : RULES) {
            if (upper.contains(rule.getKey().toUpperCase(Locale.ROOT))) {
                return rule.getValue();
            }
        }
        return null;
    }
}
