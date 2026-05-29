package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Film;
import com.example.springboot.service.FilmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "电影管理", description = "电影 CRUD、排行榜、搜索、按影院查询")
@RestController
@RequestMapping("/api/v1/films")
public class FilmController extends BaseController<Film> {

    private final FilmService filmService;

    public FilmController(FilmService filmService) {
        super(filmService);
        this.filmService = filmService;
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
