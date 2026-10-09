package com.example.springboot.mapper;

import com.example.springboot.entity.CinemaDirectory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CinemaDirectoryMapper {

    /** 名录分页/筛选（PageHelper 在此语句前 startPage）；query 的非空字段即筛选条件 */
    List<CinemaDirectory> selectAll(CinemaDirectory query);

    /** 幂等插入：poi_id 唯一，重复的静默跳过，返回本次真正插入的行数（0 或 1） */
    int insertIgnore(CinemaDirectory poi);

    /** 批量查已存在的 poi_id，供预览算「新增 / 重复」 */
    List<String> selectExistingPoiIds(@Param("poiIds") List<String> poiIds);

    List<String> distinctCities();

    List<String> distinctBrands();
}
