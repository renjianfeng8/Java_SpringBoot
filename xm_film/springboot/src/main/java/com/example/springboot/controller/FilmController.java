package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Film;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.FilmService;
import com.example.springboot.service.OrderedService;
import com.example.springboot.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "电影管理", description = "电影 CRUD、排行榜、搜索、按影院查询")
@RestController
@RequestMapping("/api/v1/films")
public class FilmController extends BaseController<Film> {

    private final FilmService filmService;
    private final RecordService recordService;
    private final OrderedService orderedService;

    public FilmController(FilmService filmService,
                          RecordService recordService,
                          OrderedService orderedService) {
        super(filmService);
        this.filmService = filmService;
        this.recordService = recordService;
        this.orderedService = orderedService;
    }

    @Override
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        ensureNoReferences(id);
        filmService.delete(id);
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        for (Integer id : ids) {
            ensureNoReferences(id);
        }
        filmService.deleteBatch(ids);
        return Result.success();
    }

    /** 影片被排片或订单引用时禁止物理删除，下架请改用 status = 停止上映 */
    private void ensureNoReferences(Integer filmId) {
        int records = recordService.countByFilmId(filmId);
        int orders = orderedService.countByFilmId(filmId);
        if (records > 0 || orders > 0) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "该影片已有 " + records + " 个排片、" + orders + " 笔订单，无法删除；如需下架请将状态改为「停止上映」");
        }
    }

    @Operation(summary = "搜索电影", description = "按标题模糊搜索")
    @GetMapping("/search")
    public Result search(@RequestParam String title) {
        return Result.success(filmService.selectByTitle(title));
    }

    @Operation(summary = "按影院查询电影", description = "查询指定影院上映的电影")
    @GetMapping("/by-cinema")
    public Result byCinema(@RequestParam Integer cinemaId,
                           @RequestParam(required = false) Integer filmId) {
        return Result.success(filmService.selectByCinema(cinemaId, filmId));
    }

    @Operation(summary = "票房排行榜 Top10")
    @GetMapping("/box-office/top")
    public Result boxOfficeTop(Film film) {
        return Result.success(filmService.getBoxOfficeTop(film));
    }

    @Operation(summary = "评分排行榜 Top5")
    @GetMapping("/mark/top")
    public Result markTop(Film film) {
        return Result.success(filmService.getMarkTop(film));
    }
}
