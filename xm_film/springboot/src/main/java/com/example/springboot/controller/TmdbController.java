package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.TmdbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

/**
 * 后台「从 TMDB 导入」的两个入口。
 *
 * 两个都是 GET，但**不是公开只读资源**：/movie/{id} 会建类型/地区/演职人员行并往磁盘写图片。
 * 因此 /api/v1/tmdb 被放进 AuthInterceptor 的 ADMIN_ONLY_PREFIXES —— 该集合对非 ADMIN 拒绝
 * **所有**方法（不像 ADMIN_WRITE_PREFIXES 只拦写方法）。这里只做「前缀级门禁」，
 * 任何放宽前缀的改动都会在 AuthInterceptorAccessTest 里失败。
 *
 * 落库不在这里：本控制器只产出一份预填值，管理员确认后仍走既有的 POST /api/v1/films，
 * 这样「建影片」始终只有一条路径。
 */
@RestController
@RequestMapping("/api/v1/tmdb")
public class TmdbController {

    private static final Logger log = LoggerFactory.getLogger(TmdbController.class);

    private final TmdbService tmdbService;

    public TmdbController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @GetMapping("/search")
    public Result search(@RequestParam String query) {
        if (query == null || query.isBlank()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请输入要搜索的影片名");
        }
        return Result.success(tmdbService.search(query));
    }

    @GetMapping("/movie/{tmdbId}")
    public Result preview(@PathVariable Integer tmdbId) {
        return Result.success(tmdbService.preview(tmdbId));
    }

    /**
     * TMDB 走不通时给出可操作的提示。本机必须经代理访问 TMDB，代理没开就是这条 ——
     * 让它落到全局兜底只会显示「系统繁忙」，管理员无从判断是自己没开代理还是对方挂了。
     */
    @ExceptionHandler(RestClientException.class)
    public Result tmdbUnreachable(RestClientException e) {
        log.warn("TMDB 请求失败: {}", e.getMessage());
        return Result.error(ErrorCode.SYSTEM_ERROR.code(), "TMDB 接口不可达，请确认本机代理已开启");
    }
}
