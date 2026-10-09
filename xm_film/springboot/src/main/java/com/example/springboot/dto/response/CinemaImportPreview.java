package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 高德导入的预览。只是一份草稿，不代表库里已经多了这些行。
 * 与 TMDB 的 TmdbImportPreview 同一思路：确认之后才走 import 落库。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaImportPreview {

    /** 抓取到的唯一 POI 数（按 poi_id 去重后） */
    private int total;

    /** 其中库中不存在、将被新增的条数 */
    private int freshCount;

    /** 其中库中已存在、将被跳过的条数 */
    private int duplicateCount;

    /** 前若干条名称，供管理员核对抓的是不是影院 */
    private List<String> sample;

    /** 降级说明：超 200 未取全、区县取不到等 */
    private List<String> warnings;
}
