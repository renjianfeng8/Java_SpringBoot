package com.example.springboot.service;

import com.example.springboot.common.FileUtil;
import com.example.springboot.entity.Film;
import com.example.springboot.entity.Type;
import com.example.springboot.mapper.FilmMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FilmService {

    @Resource
    private FilmMapper filmMapper;

    @Resource
    private  TypeService typeService;

    public List<Film> selectAll(Film film) {
        return filmMapper.selectAll(film);
    }

    public Film selectById(Integer id) {
        return filmMapper.selectById(id);
    }

    public List<Film> selectByCinema(Integer cinemaId, Integer filmId) {
        return filmMapper.selectByCinema(cinemaId, filmId);
    }

    public List<Film> selectByTitle(String title) {
        return filmMapper.selectByTitle(title);
    }

    public List<Film> selectList(Film film) {
        return filmMapper.selectAll(film);
    }

    public PageInfo<Film> selectPage(Film film, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Film> list = filmMapper.selectAll(film);
        fillFilmTypes(list);
        return new PageInfo<>(list);
    }

    public void add(Film film) {
        String jsonStr = JSON.toJSONString(film.getIds());
        film.setTypeIds(jsonStr);
        filmMapper.insert(film);
    }

    public void update(Film film) {
        String jsonStr = JSON.toJSONString(film.getIds());
        film.setTypeIds(jsonStr);
        filmMapper.updateById(film);
    }

    public void delete(Integer id) {
        filmMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.delete(id);
        }
    }


    public String uploadFile(MultipartFile file, String uploadDir) throws IOException {
        return FileUtil.uploadFile(file, uploadDir);
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

    /**
     * 批量填充电影类型名称（替代N+1循环查询）
     */
    private void fillFilmTypes(List<Film> filmList) {
        if (filmList == null || filmList.isEmpty()) {
            return;
        }
        Set<Integer> allTypeIds = new HashSet<>();
        for (Film film : filmList) {
            List<Integer> ids = JSON.parseArray(film.getTypeIds(), Integer.class);
            film.setIds(ids);
            if (ids != null) {
                allTypeIds.addAll(ids);
            }
        }
        if (allTypeIds.isEmpty()) {
            return;
        }
        List<Type> typeList = typeService.selectByIds(new ArrayList<>(allTypeIds));
        Map<Integer, String> typeMap = typeList.stream()
                .collect(Collectors.toMap(Type::getId, Type::getTitle));
        for (Film film : filmList) {
            List<String> tmpList = new ArrayList<>();
            for (Integer typeId : film.getIds()) {
                String title = typeMap.get(typeId);
                if (title != null) {
                    tmpList.add(title);
                }
            }
            film.setTypes(tmpList);
        }
    }
}
