package com.example.springboot.service;

import com.example.springboot.common.TmdbClient;
import com.example.springboot.dto.response.TmdbBackfillResult;
import com.example.springboot.entity.Film;
import com.example.springboot.mapper.FilmMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 历史影片补预告片的契约。三条口径：
 * 1) 只回写 id + video —— 整行回写会把查询聚合出来的票房灌进废弃的 box_office 静态列；
 * 2) 匹配必须片名 + 年份都对上，宁可漏配也不能给翻拍版配错预告片；
 * 3) 单部请求失败不拖垮整批，失败的记一笔、其余继续。
 */
class TmdbBackfillServiceTest {

    private static final String TRAILER = "https://www.youtube.com/embed/BdJKm16Co6M";

    private TmdbClient client;
    private FilmMapper filmMapper;
    private TmdbBackfillService service;

    @BeforeEach
    void setUp() {
        client = mock(TmdbClient.class);
        filmMapper = mock(FilmMapper.class);
        service = new TmdbBackfillService(client, filmMapper);
    }

    private Film film(int id, String title, String english, String start) {
        Film f = new Film();
        f.setId(id);
        f.setTitle(title);
        f.setEnglish(english);
        f.setStart(start);
        return f;
    }

    private TmdbClient.MovieDetail detailWith(String trailerUrl) {
        return new TmdbClient.MovieDetail("title", "orig", "1994-09-23", 139, "overview",
                null, null, null, null, List.of(), null, trailerUrl);
    }

    private TmdbClient.SearchHit hit(String title, String originalTitle, String year) {
        return new TmdbClient.SearchHit(278, title, originalTitle, year, null, null);
    }

    @Test
    void backfillsTrailerForFilmWithBlankVideo() {
        when(filmMapper.selectAll(any()))
                .thenReturn(List.of(film(2, "肖申克的救赎", "The Shawshank Redemption", "1994-09-23")));
        when(client.search("The Shawshank Redemption"))
                .thenReturn(List.of(hit("肖申克的救赎", "The Shawshank Redemption", "1994")));
        when(client.detail(278)).thenReturn(detailWith(TRAILER));

        TmdbBackfillResult result = service.backfillVideos();

        assertThat(result.getScanned()).isEqualTo(1);
        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(result.getMissed()).isEmpty();

        ArgumentCaptor<Film> captor = ArgumentCaptor.forClass(Film.class);
        verify(filmMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(2);
        assertThat(captor.getValue().getVideo()).isEqualTo(TRAILER);
        // 只带 id + video：整行回写会把聚合票房写进 box_office 静态列
        assertThat(captor.getValue().getBoxOffice()).isNull();
        assertThat(captor.getValue().getTitle()).isNull();
    }

    @Test
    void skipsFilmThatAlreadyHasVideo() {
        Film existing = film(38, "熊出没·年年有熊", "熊出没·年年有熊", "2026-02-17");
        existing.setVideo("https://www.youtube.com/embed/er2f1HxbIyg");
        when(filmMapper.selectAll(any())).thenReturn(List.of(existing));

        TmdbBackfillResult result = service.backfillVideos();

        assertThat(result.getScanned()).isZero();
        assertThat(result.getUpdated()).isZero();
        assertThat(result.getMissed()).isEmpty();
        verifyNoInteractions(client);
        verify(filmMapper, never()).updateById(any());
    }

    @Test
    void reportsNoMatchWhenTmdbHasNoExactTitleAndYear() {
        when(filmMapper.selectAll(any()))
                .thenReturn(List.of(film(2, "肖申克的救赎", "The Shawshank Redemption", "1994-09-23")));
        when(client.search(anyString())).thenReturn(List.of(hit("别的电影", "Some Other Movie", "1994")));

        TmdbBackfillResult result = service.backfillVideos();

        assertThat(result.getUpdated()).isZero();
        assertThat(result.getMissed()).singleElement().satisfies(m -> {
            assertThat(m.getId()).isEqualTo(2);
            assertThat(m.getReason()).contains("无匹配");
        });
        verify(filmMapper, never()).updateById(any());
    }

    @Test
    void reportsNoTrailerWhenMatchedFilmHasNoYouTubeTrailer() {
        when(filmMapper.selectAll(any()))
                .thenReturn(List.of(film(2, "肖申克的救赎", "The Shawshank Redemption", "1994-09-23")));
        when(client.search(anyString()))
                .thenReturn(List.of(hit("肖申克的救赎", "The Shawshank Redemption", "1994")));
        when(client.detail(278)).thenReturn(detailWith(null));

        TmdbBackfillResult result = service.backfillVideos();

        assertThat(result.getUpdated()).isZero();
        assertThat(result.getMissed()).singleElement()
                .satisfies(m -> assertThat(m.getReason()).contains("预告片"));
        verify(filmMapper, never()).updateById(any());
    }

    /** 片名相同但年份对不上视为不匹配 —— 否则会给 1994 版配到翻拍版的预告片 */
    @Test
    void doesNotMatchWhenYearDiffers() {
        when(filmMapper.selectAll(any()))
                .thenReturn(List.of(film(2, "肖申克的救赎", "The Shawshank Redemption", "1994-09-23")));
        when(client.search(anyString())).thenReturn(List.of(hit("翻拍版", "The Shawshank Redemption", "2020")));

        TmdbBackfillResult result = service.backfillVideos();

        assertThat(result.getMissed()).singleElement()
                .satisfies(m -> assertThat(m.getReason()).contains("无匹配"));
    }

    /** 一部片请求失败不该拖垮整批：失败的记一笔，其余继续补 */
    @Test
    void oneFailureDoesNotAbortTheRest() {
        when(filmMapper.selectAll(any())).thenReturn(List.of(
                film(2, "肖申克的救赎", "The Shawshank Redemption", "1994-09-23"),
                film(3, "千与千寻", "千と千尋の神隠し", "2001-07-20")));
        when(client.search("The Shawshank Redemption")).thenThrow(new RestClientException("boom"));
        when(client.search("千と千尋の神隠し"))
                .thenReturn(List.of(hit("千与千寻", "千と千尋の神隠し", "2001")));
        when(client.detail(278)).thenReturn(detailWith(TRAILER));

        TmdbBackfillResult result = service.backfillVideos();

        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(result.getMissed()).singleElement().satisfies(m -> {
            assertThat(m.getId()).isEqualTo(2);
            assertThat(m.getReason()).contains("请求失败");
        });
    }
}
