package com.example.springboot.service;

import com.example.springboot.entity.Film;
import com.example.springboot.mapper.FilmMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmServiceTest {

    @Mock
    private FilmMapper filmMapper;

    @InjectMocks
    private FilmService filmService;

    @Test
    void selectByTitleShouldDelegateToMapper() {
        when(filmMapper.selectByTitle("测试")).thenReturn(List.of(new Film()));

        List<Film> result = filmService.selectByTitle("测试");

        assertEquals(1, result.size());
        verify(filmMapper).selectByTitle("测试");
    }

    @Test
    void selectByTitleWithEmptyResultShouldReturnEmptyList() {
        when(filmMapper.selectByTitle("不存在")).thenReturn(Collections.emptyList());

        List<Film> result = filmService.selectByTitle("不存在");

        assertTrue(result.isEmpty());
    }

    @Test
    void selectByCinemaShouldDelegateToMapper() {
        when(filmMapper.selectByCinema(1, null)).thenReturn(List.of(new Film()));

        List<Film> result = filmService.selectByCinema(1, null);

        assertEquals(1, result.size());
        verify(filmMapper).selectByCinema(1, null);
    }

    @Test
    void getBoxOfficeTopWithDefaultTopNumShouldReturn10() {
        Film param = new Film();
        when(filmMapper.selectBoxOfficeTop(10)).thenReturn(List.of(new Film(), new Film()));
        when(filmMapper.selectFilmTypeJoin(anyList())).thenReturn(Collections.emptyList());

        List<Film> result = filmService.getBoxOfficeTop(param);

        assertEquals(2, result.size());
        verify(filmMapper).selectBoxOfficeTop(10);
    }

    @Test
    void getBoxOfficeTopWithCustomTopNumShouldRespectIt() {
        Film param = new Film();
        param.setTopNum(5);
        when(filmMapper.selectBoxOfficeTop(5)).thenReturn(List.of(new Film()));
        when(filmMapper.selectFilmTypeJoin(anyList())).thenReturn(Collections.emptyList());

        List<Film> result = filmService.getBoxOfficeTop(param);

        assertEquals(1, result.size());
        verify(filmMapper).selectBoxOfficeTop(5);
    }

    @Test
    void getMarkTopShouldReturnOrderedByScore() {
        Film param = new Film();
        param.setTopNum(3);
        when(filmMapper.selectMarkTop(3)).thenReturn(List.of(new Film(), new Film(), new Film()));
        when(filmMapper.selectFilmTypeJoin(anyList())).thenReturn(Collections.emptyList());

        List<Film> result = filmService.getMarkTop(param);

        assertEquals(3, result.size());
        verify(filmMapper).selectMarkTop(3);
    }

    @Test
    void addWithTypeIdsShouldInsertFilmTypes() {
        Film film = new Film();
        film.setTitle("新电影");
        film.setTypeIds(List.of(1, 2, 3));

        filmService.add(film);

        verify(filmMapper).insert(film);
        verify(filmMapper).insertFilmTypes(film.getId(), List.of(1, 2, 3));
    }

    @Test
    void addWithoutTypeIdsShouldNotInsertFilmTypes() {
        Film film = new Film();
        film.setTitle("新电影");

        filmService.add(film);

        verify(filmMapper).insert(film);
        verify(filmMapper, never()).insertFilmTypes(any(), anyList());
    }

    @Test
    void updateWithTypeIdsShouldSyncFilmTypes() {
        Film film = new Film();
        film.setId(1);
        film.setTitle("更新电影");
        film.setTypeIds(List.of(2, 3));

        filmService.update(film);

        verify(filmMapper).updateById(film);
        verify(filmMapper).deleteFilmTypesByFilmId(1);
        verify(filmMapper).insertFilmTypes(1, List.of(2, 3));
    }

    @Test
    void updateWithEmptyTypeIdsShouldDeleteOnly() {
        Film film = new Film();
        film.setId(1);
        film.setTypeIds(Collections.emptyList());

        filmService.update(film);

        verify(filmMapper).deleteFilmTypesByFilmId(1);
        verify(filmMapper, never()).insertFilmTypes(any(), anyList());
    }

    @Test
    void updateWithNullTypeIdsShouldNotTouchFilmTypes() {
        Film film = new Film();
        film.setId(1);
        film.setTypeIds(null);

        filmService.update(film);

        verify(filmMapper).updateById(film);
        verify(filmMapper, never()).deleteFilmTypesByFilmId(any());
        verify(filmMapper, never()).insertFilmTypes(any(), anyList());
    }
}
