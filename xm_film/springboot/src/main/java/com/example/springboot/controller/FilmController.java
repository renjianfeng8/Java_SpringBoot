package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Film;
import com.example.springboot.service.FilmService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 管理员管理API控制器
 * 提供管理员信息的增删改查及分页查询功能
 */
@RestController
@RequestMapping("/film")
public class FilmController {

    @Resource
    private FilmService filmService;

    // 注入文件上传目录配置
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 注入文件访问前缀配置
    @Value("${file.access-prefix}")
    private String accessPrefix;




    @GetMapping("/selectAll")
    public Result selectAll(Film film) {
        List<Film> list = filmService.selectAll(film);
        return Result.success(list);
    }


    /**
     * 查询票房排行榜（按票房降序排列，取前N名）
     * @param film 可包含筛选条件（如电影类型、上映时间等）和排名数量限制（topN）
     * @return 返回符合条件的票房排行榜列表
     */
    @GetMapping("/getAllBoxOfficeTop")
    public Result getAllBoxOfficeTop(Film film) {
        // 调用服务层方法查询票房排行，传入筛选条件
        List<Film> boxOfficeTopList = filmService.getBoxOfficeTop(film);
        return Result.success(boxOfficeTopList);
    }


    /**
     * 查询评分排行榜（按评分降序排列，取前N名）
     * @param film 可包含筛选条件（如电影类型、上映时间等）和排名数量限制（topN）
     * @return 返回符合条件的评分排行榜列表
     */
    @GetMapping("/getAllMarkTop")
    public Result getAllMarkTop(Film film) {
        // 调用服务层方法查询评分排行，传入筛选条件
        List<Film> markTopList = filmService.getMarkTop(film);
        return Result.success(markTopList);
    }


    @GetMapping("/selectByTitle")
    public Result selectByTitle(@RequestParam("title") String title) {
        List<Film> filmList = filmService.selectByTitle(title);
        return Result.success(filmList);
    }


    @GetMapping("/selectList")
    public Result selectList(Film film) {
        List<Film> list = filmService.selectList(film);
        return Result.success(list);
    }


    @GetMapping("/selectPage")
    public Result selectPage(Film film,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Film> pageInfo = filmService.selectPage(film, pageNum, pageSize);
        return Result.success(pageInfo);
    }


    @GetMapping("/selectById/{id}")
    public Result selectByID(@PathVariable Integer id) {
        Film film = filmService.selectById(id);
        return Result.success(film);
    }


    @PostMapping("/add")
    public Result add(@RequestBody Film film) {
        filmService.add(film);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Film film) {
        filmService.update(film);
        return Result.success();
    }


    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        filmService.delete(id);
        return Result.success();
    }


    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        filmService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/selectByCinema")
    public Result selectByCinema(
            @RequestParam Integer cinemaId, // 影院ID（必传）
            @RequestParam(defaultValue = "1") Integer pageNum, // 页码，默认第1页
            @RequestParam(defaultValue = "10") Integer pageSize, // 每页条数，默认10条
            @RequestParam(required = false) Integer filmId) { // 电影ID（可选，用于筛选特定电影）
        List<Film> films = filmService.selectByCinema(cinemaId, filmId);
        return Result.success(films);
    }


    /**
     * 文件上传接口
     */
    @PostMapping("/upload")
    public Result uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("400", "上传文件不能为空");
            }

            String fileName = filmService.uploadFile(file, uploadDir);

            String fileUrl = accessPrefix + fileName;

            // 打印调试信息
            System.out.println("生成的文件URL: " + fileUrl);

            return Result.success(fileUrl);
        } catch (IllegalArgumentException e) {
            return Result.error("400", e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("500", "文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "服务器内部错误");
        }
    }

    
}