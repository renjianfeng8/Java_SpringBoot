package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Film;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.FilmService;
import com.example.springboot.service.OrderedService;
import com.example.springboot.service.RecordService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * 今日票房，前台首页公开展示，匿名可读 —— 路径落在 /api/v1/films 这个已在
     * PUBLIC_READ_PREFIXES 内的前缀下，因此不需要为它新增任何放行规则。
     * 返回 {total, updatedAt}，口径见 OrderedService.todayPaidRevenue。
     */
    @GetMapping("/box-office/today")
    public Result todayBoxOffice() {
        return Result.success(orderedService.todayPaidRevenue());
    }

    @GetMapping("/mark/top")
    public Result markTop(Film film) {
        return Result.success(filmService.getMarkTop(film));
    }
}
