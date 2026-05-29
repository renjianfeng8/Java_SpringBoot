package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Film;
import com.example.springboot.entity.Type;
import com.example.springboot.mapper.FilmMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FilmService extends BaseService<Film> {

    @Resource
    private FilmMapper filmMapper;

    @Override
    protected BaseMapper<Film> mapper() {
        return filmMapper;
    }

    @Override
    public List<Film> selectAll(Film film) {
        List<Film> list = mapper().selectAll(film);
        fillFilmTypes(list);
        return list;
    }

    @Override
    public Film selectById(Integer id) {
        Film film = mapper().selectById(id);
        if (film != null) {
            fillFilmTypes(List.of(film));
        }
        return film;
    }

    public List<Film> selectByCinema(Integer cinemaId, Integer filmId) {
        return filmMapper.selectByCinema(cinemaId, filmId);
    }

    public List<Film> selectByTitle(String title) {
        return filmMapper.selectByTitle(title);
    }

    public PageInfo<Film> selectPage(Film film, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Film> list = filmMapper.selectAll(film);
        fillFilmTypes(list);
        return new PageInfo<>(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Film film) {
        mapper().insert(film);
        if (film.getTypeIds() != null && !film.getTypeIds().isEmpty()) {
            filmMapper.insertFilmTypes(film.getId(), film.getTypeIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Film film) {
        mapper().updateById(film);
        if (film.getTypeIds() != null) {
            filmMapper.deleteFilmTypesByFilmId(film.getId());
            if (!film.getTypeIds().isEmpty()) {
                filmMapper.insertFilmTypes(film.getId(), film.getTypeIds());
            }
        }
    }

    public List<Film> getBoxOfficeTop(Film film) {
        int topNum = film.getTopNum() != null ? film.getTopNum() : 10;
        List<Film> result = filmMapper.selectBoxOfficeTop(topNum);
        fillFilmTypes(result);
        return result;
    }

    public List<Film> getMarkTop(Film film) {
        int topNum = film.getTopNum() != null ? film.getTopNum() : 10;
        List<Film> result = filmMapper.selectMarkTop(topNum);
        fillFilmTypes(result);
        return result;
    }

    private void fillFilmTypes(List<Film> filmList) {
        if (filmList == null || filmList.isEmpty()) {
            return;
        }
        List<Integer> filmIds = filmList.stream().map(Film::getId).collect(Collectors.toList());
        List<Map<String, Object>> rows = filmMapper.selectFilmTypeJoin(filmIds);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Integer, List<Integer>> filmTypeIdMap = new LinkedHashMap<>();
        Map<Integer, List<Type>> filmTypeMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Integer filmId = ((Number) row.get("film_id")).intValue();
            Integer typeId = ((Number) row.get("type_id")).intValue();
            String typeTitle = (String) row.get("type_title");
            filmTypeIdMap.computeIfAbsent(filmId, k -> new ArrayList<>()).add(typeId);
            filmTypeMap.computeIfAbsent(filmId, k -> new ArrayList<>()).add(new Type(typeId, typeTitle));
        }
        for (Film film : filmList) {
            film.setTypeIds(filmTypeIdMap.get(film.getId()));
            film.setTypeList(filmTypeMap.get(film.getId()));
        }
    }
}
