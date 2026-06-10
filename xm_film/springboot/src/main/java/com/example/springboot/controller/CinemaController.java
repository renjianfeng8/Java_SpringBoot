package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.Cinema;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.CinemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

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

    @Override
    @PutMapping
    public Result update(@RequestBody Cinema cinema) {
        if (isAdmin()) {
            cinemaService.update(cinema);
            return Result.success();
        }
        if (isCinema()) {
            cinema.setId(currentUserId());
            cinema.setStatus(null);
            cinemaService.update(cinema);
            return Result.success();
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
    }

    @Override
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        requireAdmin();
        cinemaService.delete(id);
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        requireAdmin();
        cinemaService.deleteBatch(ids);
        return Result.success();
    }

    private boolean isAdmin() {
        return "ADMIN".equals(currentRole());
    }

    private boolean isCinema() {
        return "CINEMA".equals(currentRole());
    }

    private void requireAdmin() {
        if (!isAdmin()) {
            throw new CustomException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }

    private String currentRole() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : (String) request.getAttribute("role");
    }

    private Integer currentUserId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String userId = (String) request.getAttribute("userId");
        return userId == null ? null : Integer.valueOf(userId);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
