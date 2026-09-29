package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "数据统计", description = "后台可视化大盘的实时统计（只读聚合）")
@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Operation(summary = "大盘总览", description = "影院状态分布 + 影片类型分布，均由数据库实时聚合；仅管理员可见")
    @GetMapping("/overview")
    public Result overview() {
        if (!"ADMIN".equals(AuthContext.role())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "仅管理员可查看统计数据");
        }
        return Result.success(statisticsService.overview());
    }
}
