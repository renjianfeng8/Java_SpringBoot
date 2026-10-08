package com.example.springboot.common;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * TMDB 原始值 → 影片表单取值的映射。
 *
 * 纯函数、无 Spring 依赖。目标取值域由表单本身固定，不由 TMDB 决定：
 * 语言是 7 个选项、状态是 3 个选项、类型最多 4 个。映射不上时一律留空或归「其他」，
 * 不新增选项 —— 表单加选项是另一件事，不该被一次导入悄悄改掉。
 */
public final class TmdbMapping {

    private TmdbMapping() {
    }

    /**
     * 表单「电影语言」7 个选项中可由 TMDB 语言码确定的 6 个。
     * `cn` 是 TMDB 自己的分配，表示粤语（并非 ISO 639-1 的原生含义），实测自 /configuration/languages。
     */
    private static final Map<String, String> LANGUAGE_OPTIONS = Map.of(
            "zh", "普通话",
            "cn", "港语",
            "en", "英语",
            "fr", "法语",
            "ja", "日语",
            "ru", "俄语"
    );

    private static final String OTHER_LANGUAGE = "其他";

    /** 与影片表单 handleTypeChange 的硬上限一致 */
    public static final int MAX_GENRES = 4;

    /**
     * 语言码 → 表单选项。缺失返回空串，由表单必填校验去拦；
     * 有值但不在上表内则归「其他」—— 表单没有第 8 个选项。
     */
    public static String languageFormOption(String iso6391) {
        if (iso6391 == null || iso6391.isBlank()) {
            return "";
        }
        return LANGUAGE_OPTIONS.getOrDefault(iso6391.trim().toLowerCase(), OTHER_LANGUAGE);
    }

    /**
     * 上映日期 → 影片状态。TMDB 对未定档影片返回空串，解析失败一律留空而不猜 ——
     * 猜成「已上映」会把未定档影片推上前台。
     */
    public static String deriveStatus(String releaseDate, LocalDate today) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return "";
        }
        try {
            return LocalDate.parse(releaseDate).isAfter(today) ? "待上映" : "已上映";
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    /**
     * 截断到表单上限，保留 TMDB 给出的顺序（其 genre 顺序即重要程度）。
     * 泛型是为了能在建 type 行**之前**先截类型名 —— 反过来先建行再截，会留下
     * 用户根本没选的多余类型行。
     */
    public static <T> List<T> trimToFormLimit(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.size() <= MAX_GENRES ? values : List.copyOf(values.subList(0, MAX_GENRES));
    }
}
