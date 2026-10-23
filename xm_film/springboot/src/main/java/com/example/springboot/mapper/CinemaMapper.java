package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.dto.response.CinemaRegionItemView;
import com.example.springboot.dto.response.NameValueView;
import com.example.springboot.entity.Cinema;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CinemaMapper extends BaseMapper<Cinema> {

    /** 删除守卫用：锁影院行（须在 @Transactional 内调用，否则退化为普通读） */
    Cinema selectByIdForUpdate(Integer id);

    Cinema selectByUsername(String username);

    List<Cinema> selectByFilmId(@Param("cinema") Cinema cinema,
                                @Param("filmId") Integer filmId,
                                @Param("approvedOnly") Boolean approvedOnly);

    void updatePassword(Cinema cinema);

    /** 只改封面地址：补图专用，避免整行 update 覆盖别的字段 */
    void updateAvatar(@Param("id") Integer id, @Param("avatar") String avatar);

    /** 缺封面且能关联高德名录的影院 */
    List<MissingCoverTarget> selectMissingCovers();

    /** 补图目标：影院 id + 高德 POI id */
    record MissingCoverTarget(Integer cinemaId, String poiId) {
    }

    /** 大盘统计：影院状态分布（[{name, value}]，由 cinema 实时计数） */
    List<NameValueView> countGroupByStatus();

    /** 前台区域级联：distinct 的 {city, district} 行，仅已审核已关联影院 */
    List<CinemaRegionItemView> selectRegions();

    /** 已在 cinema 中关联的这些名录 id（去重），供「从影院名录生成影院」跳过已导入行 */
    List<Integer> selectLinkedDirectoryIds(List<Integer> ids);

}
