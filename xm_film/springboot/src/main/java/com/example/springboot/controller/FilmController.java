package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Film;
import com.example.springboot.service.FilmService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/films")
public class FilmController extends BaseController<Film> {

    private final FilmService filmService;

    public FilmController(FilmService filmService) {
        super(filmService);
        this.filmService = filmService;
    }

    @GetMapping("/search")
    public Result search(@RequestParam String title) {
        return Result.success(filmService.selectByTitle(title));
    }

    @GetMapping("/by-cinema")
    public Result byCinema(@RequestParam Integer cinemaId,
                           @RequestParam(required = false) Integer filmId) {
        return Result.success(filmService.selectByCinema(cinemaId, filmId));
    }

    @GetMapping("/box-office/top")
    public Result boxOfficeTop(Film film) {
        return Result.success(filmService.getBoxOfficeTop(film));
    }

    @GetMapping("/mark/top")
    public Result markTop(Film film) {
        return Result.success(filmService.getMarkTop(film));
    }
}
