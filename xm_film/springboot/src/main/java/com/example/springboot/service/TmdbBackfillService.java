package com.example.springboot.service;

import com.example.springboot.common.TmdbClient;
import com.example.springboot.dto.response.TmdbBackfillResult;
import com.example.springboot.entity.Film;
import com.example.springboot.mapper.FilmMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * 给历史影片补预告片。
 *
 * 影片的预告片地址是导入时算出来、随影片一起写进 {@code film.video} 的，只写那一次。
 * 接入预告片之前导入的影片该列是空的，本服务按片名回查 TMDB 再写回，作为一次性补救。
 *
 * 匹配从严：要求 TMDB 命中项的片名（原始名或显示名）与影片 {@code english} 全等、
 * 且年份一致才采纳 —— 宁可漏配（留空、列进 missed）也不能给翻拍版配错预告片。
 *
 * 非事务、逐部独立提交：一部请求失败只记一笔并继续，不回滚已补好的其它影片。
 */
@Service
public class TmdbBackfillService {

    private static final Logger log = LoggerFactory.getLogger(TmdbBackfillService.class);

    private final TmdbClient client;
    private final FilmMapper filmMapper;

    public TmdbBackfillService(TmdbClient client, FilmMapper filmMapper) {
        this.client = client;
        this.filmMapper = filmMapper;
    }

    public TmdbBackfillResult backfillVideos() {
        List<Film> films = filmMapper.selectAll(new Film());
        List<TmdbBackfillResult.Miss> missed = new ArrayList<>();
        int scanned = 0;
        int updated = 0;

        for (Film film : films) {
            if (hasVideo(film)) {
                continue;
            }
            scanned++;
            try {
                Resolved resolved = resolve(film);
                if (resolved.url() == null) {
                    missed.add(new TmdbBackfillResult.Miss(film.getId(), film.getTitle(), resolved.reason()));
                    continue;
                }
                filmMapper.updateById(videoPatch(film.getId(), resolved.url()));
                updated++;
            } catch (RestClientException e) {
                log.warn("TMDB 回填预告片失败 film={}: {}", film.getId(), e.getMessage());
                missed.add(new TmdbBackfillResult.Miss(film.getId(), film.getTitle(), "TMDB 请求失败"));
            }
        }

        return new TmdbBackfillResult(scanned, updated, missed);
    }

    /**
     * 只带 id 与 video 的更新实体。绝不能把查询回来的整行直接回写：
     * 那会把查询聚合出的票房灌进已废弃的 film.box_office 静态列（见 Film 实体的注释）。
     */
    private static Film videoPatch(Integer id, String video) {
        Film patch = new Film();
        patch.setId(id);
        patch.setVideo(video);
        return patch;
    }

    private Resolved resolve(Film film) {
        String query = firstNonBlank(film.getEnglish(), film.getTitle());
        if (query == null) {
            return new Resolved(null, "无片名可查");
        }
        String year = yearOf(film.getStart());
        for (TmdbClient.SearchHit hit : client.search(query)) {
            if (matches(hit, query, year)) {
                String url = client.detail(hit.tmdbId()).trailerUrl();
                return url != null ? new Resolved(url, null) : new Resolved(null, "TMDB 无预告片");
            }
        }
        return new Resolved(null, "TMDB 无匹配");
    }

    /** 片名全等 + 年份一致才算命中；影片没有上映年份时退化为只对片名 */
    private static boolean matches(TmdbClient.SearchHit hit, String query, String year) {
        boolean titleMatch = query.equalsIgnoreCase(hit.originalTitle()) || query.equalsIgnoreCase(hit.title());
        if (!titleMatch) {
            return false;
        }
        return year == null || year.equals(hit.year());
    }

    private static boolean hasVideo(Film film) {
        return film.getVideo() != null && !film.getVideo().isBlank();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return (second != null && !second.isBlank()) ? second : null;
    }

    private static String yearOf(String date) {
        return (date == null || date.length() < 4) ? null : date.substring(0, 4);
    }

    private record Resolved(String url, String reason) {
    }
}
