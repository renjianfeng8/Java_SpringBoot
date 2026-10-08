package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Area;

public interface AreaMapper extends BaseMapper<Area> {

    /**
     * 按标题**精确**匹配，供 TMDB 导入做「有就复用、没有才建」的判重。
     * 不复用 selectAll 的 LIKE —— 「美国」会命中「美国（合拍）」，那样导入会把地区越并越少。
     * area.title 无唯一索引，故语句内 LIMIT 1，重复标题不至于抛 TooManyResults。
     */
    Area selectByTitleExact(String title);

}
