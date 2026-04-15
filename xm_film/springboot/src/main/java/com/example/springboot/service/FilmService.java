package com.example.springboot.service;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        System.out.println(film);
        return null;
    }

    public PageInfo<Film> selectPage(Film film, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Film> list = filmMapper.selectAll(film);
        for (Film dbFilm : list) {
            // 将JSON字符串转换为List<Integer>
            List<Integer> ids = JSON.parseArray(dbFilm.getTypeIds(), Integer.class);
            dbFilm.setIds(ids);

            List<String> tmpList = new ArrayList<>();
            for (Integer typeId : ids) {
                Type type = typeService.selectById(typeId);
                if (!ObjectUtils.isEmpty(type)) {
                    tmpList.add(type.getTitle());
                }
            }
            dbFilm.setTypes(tmpList);
        }
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


    /**
     * 文件上传功能
     * @param file 上传的文件
     * @param uploadDir 文件存储目录
     * @return 存储后的文件相对路径
     * @throws IOException 文件操作异常
     */
    public String uploadFile(MultipartFile file, String uploadDir) throws IOException {
        // 检查上传文件是否为空
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        // 检查并创建上传目录
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IOException("无法创建上传目录: " + uploadDir);
            }
        }

        // 生成唯一文件名，避免文件重名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID() + fileExtension;

        // 构建完整文件路径
        Path filePath = Paths.get(uploadDir, uniqueFileName);

        try {
            // 保存文件到指定路径
            Files.write(filePath, file.getBytes());
            // 返回文件相对路径（相对于上传目录）
            return uniqueFileName;
        } catch (IOException e) {
            // 上传失败时，尝试删除已创建的空文件
            if (Files.exists(filePath)) {
                Files.deleteIfExists(filePath);
            }
            throw new IOException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件扩展名（包括点号）
     * @param fileName 文件名
     * @return 文件扩展名（例如：.jpg, .pdf）
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return "";
        }

        return fileName.substring(lastIndex);
    }


    public List<Film> getBoxOfficeTop(Film film) {
        // 查询符合条件的电影列表
        List<Film> allFilms = filmMapper.selectAll(film);

        // 按票房降序排序（针对double类型的boxOffice字段）
        allFilms.sort((f1, f2) -> {
            // 基本数据类型double不能为null，直接比较数值
            // 降序排序（大的在前）
            return Double.compare(f2.getBoxOffice(), f1.getBoxOffice());
        });

        // 获取需要返回的数量
        int topNum = film.getTopNum() != null ? film.getTopNum() : 10;
        int limit = Math.min(topNum, allFilms.size());

        // 处理电影类型信息
        List<Film> result = allFilms.subList(0, limit);
        for (Film dbFilm : result) {
            List<Integer> ids = JSON.parseArray(dbFilm.getTypeIds(), Integer.class);
            dbFilm.setIds(ids);

            List<String> tmpList = new ArrayList<>();
            for (Integer typeId : ids) {
                Type type = typeService.selectById(typeId);
                if (!ObjectUtils.isEmpty(type)) {
                    tmpList.add(type.getTitle());
                }
            }
            dbFilm.setTypes(tmpList);
        }

        return result;
    }

    public List<Film> getMarkTop(Film film) {
        // 查询符合条件的电影列表
        List<Film> allFilms = filmMapper.selectAll(film);

        // 按评分降序排序（针对score字段）
        allFilms.sort((f1, f2) -> {
            // 处理可能的null值（如果score是Double类型）
            if (f1.getScore() == null && f2.getScore() == null) return 0;
            if (f1.getScore() == null) return 1;
            if (f2.getScore() == null) return -1;
            // 降序排序（高分在前）
            return Double.compare(f2.getScore(), f1.getScore());
        });

        // 获取需要返回的数量
        int topNum = film.getTopNum() != null ? film.getTopNum() : 10;
        int limit = Math.min(topNum, allFilms.size());

        // 处理电影类型信息（与其他查询保持一致逻辑）
        List<Film> result = allFilms.subList(0, limit);
        for (Film dbFilm : result) {
            List<Integer> ids = JSON.parseArray(dbFilm.getTypeIds(), Integer.class);
            dbFilm.setIds(ids);

            List<String> tmpList = new ArrayList<>();
            for (Integer typeId : ids) {
                Type type = typeService.selectById(typeId);
                if (!ObjectUtils.isEmpty(type)) {
                    tmpList.add(type.getTitle());
                }
            }
            dbFilm.setTypes(tmpList);
        }

        return result;
    }
}
