package com.example.springboot.mapper;

import com.example.springboot.entity.CinemaDirectory;

import java.util.List;

public interface CinemaDirectoryMapper {

    /** 名录分页/筛选（PageHelper 在此语句前 startPage）；query 的非空字段即筛选条件 */
    List<CinemaDirectory> selectAll(CinemaDirectory query);

    /** 幂等插入：poi_id 唯一，重复的静默跳过，返回本次真正插入的行数（0 或 1） */
    int insertIgnore(CinemaDirectory poi);

    /** 批量查已存在的 poi_id，供预览算「新增 / 重复」 */
    List<String> selectExistingPoiIds(List<String> poiIds);

    /** 按名录 id 批量取行，供「从名录生成影院」 */
    List<CinemaDirectory> selectByIds(List<Integer> ids);

    List<String> distinctCities();

    List<String> distinctBrands();
}
