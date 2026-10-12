package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.dto.request.FilmRequest;
import com.example.springboot.dto.response.FilmView;
import com.example.springboot.entity.Film;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.FilmService;
import com.example.springboot.service.OrderedService;
import com.example.springboot.service.RecordService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/films")
public class FilmController extends BaseController<Film, FilmRequest, FilmView> {

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
    protected Film toEntity(FilmRequest request) {
        Film film = new Film();
        film.setId(request.getId());
        film.setTitle(request.getTitle());
        film.setEnglish(request.getEnglish());
        film.setStart(request.getStart());
        film.setTime(request.getTime());
        film.setLanguage(request.getLanguage());
        film.setResolution(request.getResolution());
        film.setContent(request.getContent());
        film.setImg(request.getImg());
        film.setEmployee(request.getEmployee());
        film.setAreaId(request.getAreaId());
        film.setStatus(request.getStatus());
        film.setScore(request.getScore());
        film.setYear(request.getYear());
        film.setAreaName(request.getAreaName());
        film.setIds(request.getIds());
        film.setTypeId(request.getTypeId());
        film.setBoxOffice(request.getBoxOffice());
        film.setActorId(request.getActorId());
        film.setActorInfo(request.getActorInfo());
        film.setTopNum(request.getTopNum());
        film.setVideo(request.getVideo());
        film.setTypeIds(request.getTypeIds());
        film.setTypeList(request.getTypeList());
        return film;
    }

    @Override
    protected FilmView toView(Film film) {
        FilmView view = new FilmView();
        view.setId(film.getId());
        view.setTitle(film.getTitle());
        view.setEnglish(film.getEnglish());
        view.setStart(film.getStart());
        view.setTime(film.getTime());
        view.setLanguage(film.getLanguage());
        view.setResolution(film.getResolution());
        view.setContent(film.getContent());
        view.setImg(film.getImg());
        view.setEmployee(film.getEmployee());
        view.setAreaId(film.getAreaId());
        view.setStatus(film.getStatus());
        view.setScore(film.getScore());
        view.setYear(film.getYear());
        view.setAreaName(film.getAreaName());
        view.setIds(film.getIds());
        view.setTypeId(film.getTypeId());
        view.setBoxOffice(film.getBoxOffice());
        view.setActorId(film.getActorId());
        view.setActorInfo(film.getActorInfo());
        view.setTopNum(film.getTopNum());
        view.setVideo(film.getVideo());
        view.setTypeIds(film.getTypeIds());
        view.setTypeList(film.getTypeList());
        return view;
    }

    @Override
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        ensureNoReferences(id);
        filmService.delete(id);
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
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
    public Result<List<FilmView>> search(@RequestParam String title) {
        return Result.success(filmService.selectByTitle(title).stream().map(this::toView).toList());
    }

    @GetMapping("/by-cinema")
    public Result<List<FilmView>> byCinema(@RequestParam Integer cinemaId,
                           @RequestParam(required = false) Integer filmId) {
        return Result.success(filmService.selectByCinema(cinemaId, filmId).stream().map(this::toView).toList());
    }

    @GetMapping("/box-office/top")
    public Result<List<FilmView>> boxOfficeTop(FilmRequest query) {
        return Result.success(filmService.getBoxOfficeTop(toEntity(query)).stream().map(this::toView).toList());
    }

    /**
     * 今日票房，前台首页公开展示，匿名可读 —— 路径落在 /api/v1/films 这个已在
     * PUBLIC_READ_PREFIXES 内的前缀下，因此不需要为它新增任何放行规则。
     * 返回 {total, updatedAt}，口径见 OrderedService.todayPaidRevenue。
     */
    @GetMapping("/box-office/today")
    public Result<Map<String, Object>> todayBoxOffice() {
        return Result.success(orderedService.todayPaidRevenue());
    }

    @GetMapping("/mark/top")
    public Result<List<FilmView>> markTop(FilmRequest query) {
        return Result.success(filmService.getMarkTop(toEntity(query)).stream().map(this::toView).toList());
    }
}
