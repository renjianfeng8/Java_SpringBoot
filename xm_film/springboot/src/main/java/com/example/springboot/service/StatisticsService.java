package com.example.springboot.service;

import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.mapper.FilmMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 后台可视化大盘的统计口径：全部由数据库实时聚合，不落快照、不缓存、不造数。
 * 无数据的维度返回空列表，由前端渲染「暂无数据」占位。
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    @Resource
    private CinemaMapper cinemaMapper;

    @Resource
    private FilmMapper filmMapper;

    /** 影院状态分布 + 影片类型分布，{name, value} 列表直接喂 ECharts */
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cinemaStatus", cinemaMapper.countGroupByStatus());
        result.put("filmType", filmMapper.countGroupByType());
        return result;
    }
}
