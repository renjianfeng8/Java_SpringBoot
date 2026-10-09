package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.dto.request.CinemaImportRequest;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.CinemaDirectoryImportService;
import com.example.springboot.service.CinemaDirectoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 影院名录。GET 是公开只读资源；/import 与 /import/preview 会触发高德抓取与写库，
 * 由 AuthInterceptor 的 ADMIN_WRITE_PREFIXES 拦在非管理员之外（与本仓 film 写同款前缀级门禁）。
 * 目录行只从高德来，没有第二条写路径。
 */
@RestController
@RequestMapping("/api/v1/cinema-directory")
public class CinemaDirectoryController {

    private final CinemaDirectoryService directoryService;
    private final CinemaDirectoryImportService importService;

    public CinemaDirectoryController(CinemaDirectoryService directoryService,
                                     CinemaDirectoryImportService importService) {
        this.directoryService = directoryService;
        this.importService = importService;
    }

    @GetMapping("/page")
    public Result page(CinemaDirectory query,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.success(directoryService.selectPage(query, pageNum, pageSize));
    }

    /** 前台筛选下拉与后台导入选城都用得到 */
    @GetMapping("/filters")
    public Result filters() {
        return Result.success(Map.of(
                "cities", directoryService.distinctCities(),
                "brands", directoryService.distinctBrands()));
    }

    @PostMapping("/import/preview")
    public Result preview(@RequestBody CinemaImportRequest request) {
        return Result.success(importService.preview(requireCities(request)));
    }

    @PostMapping("/import")
    public Result doImport(@RequestBody CinemaImportRequest request) {
        return Result.success(importService.importAll(requireCities(request)));
    }

    private static List<String> requireCities(CinemaImportRequest request) {
        if (request == null || request.getCities() == null || request.getCities().isEmpty()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请至少选择一个城市");
        }
        return request.getCities();
    }
}
