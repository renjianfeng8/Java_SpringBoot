package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 补预告片的结果。scanned 是本次纳入处理的影片数（已有预告片的会跳过、不计入），
 * missed 逐条给出「哪部、为什么没补上」，供管理员决定是否手工处理。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TmdbBackfillResult {

    private int scanned;
    private int updated;
    private List<Miss> missed;

    /** 一部没补上的影片及原因（TMDB 无匹配 / TMDB 无预告片 / TMDB 请求失败） */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Miss {
        private Integer id;
        private String title;
        private String reason;
    }
}
