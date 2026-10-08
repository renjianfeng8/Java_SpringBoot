package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Type;

import java.util.List;

public interface TypeMapper extends BaseMapper<Type> {

    List<Type> selectByIds(List<Integer> ids);

    /**
     * 按标题**精确**匹配，供 TMDB 导入做「有就复用、没有才建」的判重。
     * 不复用 selectAll 的 LIKE —— 「剧情」会命中「剧情片」，那样导入会把类型越并越少。
     * type.title 无唯一索引，故语句内 LIMIT 1，重复标题不至于抛 TooManyResults。
     */
    Type selectByTitleExact(String title);

}
