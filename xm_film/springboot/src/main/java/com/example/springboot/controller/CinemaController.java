package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Cinema;
import com.example.springboot.service.CinemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Tag(name = "影院管理", description = "影院 CRUD、分页查询（支持按电影筛选）")
@RestController
@RequestMapping("/api/v1/cinemas")
public class CinemaController extends BaseController<Cinema> {

    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        super(cinemaService);
        this.cinemaService = cinemaService;
    }

    @Operation(summary = "分页查询影院", description = "支持按电影ID筛选正在上映该电影的影院")
    @Override
    @GetMapping("/page")
    public Result page(Cinema cinema,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer filmId = null;
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String filmIdStr = attrs.getRequest().getParameter("filmId");
            if (filmIdStr != null) {
                filmId = Integer.valueOf(filmIdStr);
            }
        }
        return Result.success(cinemaService.selectPage(cinema, filmId, pageNum, pageSize));
    }
}
