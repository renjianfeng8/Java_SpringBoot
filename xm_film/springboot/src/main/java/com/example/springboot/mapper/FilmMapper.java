package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.dto.response.FilmTypeJoinView;
import com.example.springboot.dto.response.NameValueView;
import com.example.springboot.entity.Film;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FilmMapper extends BaseMapper<Film> {

    /** 删除守卫用：锁影片行（须在 @Transactional 内调用，否则退化为普通读） */
    Film selectByIdForUpdate(Integer id);

    List<Film> selectByTitle(String title);

    /** 补预告片用：只取 video 为空的行，避开 selectAll 的票房聚合与 JOIN */
    List<Film> selectMissingVideo();

    /** 下拉选项用：只取 id/title，避开 selectAll 的全列（含 content 长文本）与票房聚合 */
    List<Film> selectOptions();

    List<Film> selectByCinema(@Param("cinemaId") Integer cinemaId, @Param("filmId") Integer filmId);

    List<Film> selectBoxOfficeTop(Integer topNum);

    /** 评分榜：只含真正有评价的影片（SQL 内含 EXISTS(mark) 谓词） */
    List<Film> selectMarkTop(Integer topNum);

    /** 按 mark 评价均分回写影片评分；film.score 的唯一写者，无评价时写 NULL */
    void recalculateScore(Integer filmId);

    List<FilmTypeJoinView> selectFilmTypeJoin(List<Integer> filmIds);

    /** 大盘统计：影片类型分布（[{name, value}]，由 film_type 实时计数） */
    List<NameValueView> countGroupByType();

    void insertFilmTypes(@Param("filmId") Integer filmId, @Param("typeIds") List<Integer> typeIds);

    void deleteFilmTypesByFilmId(Integer filmId);
}
