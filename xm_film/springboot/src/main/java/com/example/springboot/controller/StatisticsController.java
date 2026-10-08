package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public Result overview() {
        if (!"ADMIN".equals(AuthContext.role())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "仅管理员可查看统计数据");
        }
        return Result.success(statisticsService.overview());
    }
}
