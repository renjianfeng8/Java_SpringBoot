package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.dto.request.CinemaDirectoryRequest;
import com.example.springboot.dto.request.CinemaImportRequest;
import com.example.springboot.dto.response.CinemaDirectoryView;
import com.example.springboot.dto.response.CinemaImportView;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.CinemaDirectoryImportService;
import com.example.springboot.service.CinemaDirectoryService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 影院名录。整条前缀仅管理员可用（见 AuthInterceptor 的 ADMIN_ONLY_PREFIXES）：
 * 读端 page/filters 只服务后台管理页，写端 /import 会触发高德抓取与建行。
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
    public Result<PageInfo<CinemaDirectoryView>> page(CinemaDirectoryRequest query,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "12") Integer pageSize) {
        PageInfo<CinemaDirectory> src = directoryService.selectPage(toEntity(query), pageNum, pageSize);
        PageInfo<CinemaDirectoryView> dst = new PageInfo<>();
        BeanUtils.copyProperties(src, dst);
        dst.setList(src.getList().stream().map(this::toView).toList());
        return Result.success(dst);
    }

    /** 后台名录页筛选与导入选城下拉的数据源 */
    @GetMapping("/filters")
    public Result<Map<String, List<String>>> filters() {
        return Result.success(Map.of(
                "cities", directoryService.distinctCities(),
                "brands", directoryService.distinctBrands()));
    }

    @PostMapping("/import/preview")
    public Result<CinemaImportView> preview(@RequestBody CinemaImportRequest request) {
        return Result.success(importService.preview(requireCities(request)));
    }

    @PostMapping("/import")
    public Result<Integer> doImport(@RequestBody CinemaImportRequest request) {
        return Result.success(importService.importAll(requireCities(request)));
    }

    /** 请求 DTO → 实体：查询条件（含派生筛选字段 linked）交给 Service/DAO */
    private CinemaDirectory toEntity(CinemaDirectoryRequest request) {
        CinemaDirectory entity = new CinemaDirectory();
        entity.setId(request.getId());
        entity.setPoiId(request.getPoiId());
        entity.setName(request.getName());
        entity.setBrand(request.getBrand());
        entity.setProvince(request.getProvince());
        entity.setCity(request.getCity());
        entity.setDistrict(request.getDistrict());
        entity.setAddress(request.getAddress());
        entity.setPhone(request.getPhone());
        entity.setSource(request.getSource());
        entity.setCreateTime(request.getCreateTime());
        entity.setLinked(request.getLinked());
        return entity;
    }

    /** 实体 → 响应视图 */
    private CinemaDirectoryView toView(CinemaDirectory entity) {
        CinemaDirectoryView view = new CinemaDirectoryView();
        view.setId(entity.getId());
        view.setPoiId(entity.getPoiId());
        view.setName(entity.getName());
        view.setBrand(entity.getBrand());
        view.setProvince(entity.getProvince());
        view.setCity(entity.getCity());
        view.setDistrict(entity.getDistrict());
        view.setAddress(entity.getAddress());
        view.setPhone(entity.getPhone());
        view.setSource(entity.getSource());
        view.setCreateTime(entity.getCreateTime());
        view.setLinked(entity.getLinked());
        return view;
    }

    private static List<String> requireCities(CinemaImportRequest request) {
        if (request == null || request.getCities() == null || request.getCities().isEmpty()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请至少选择一个城市");
        }
        return request.getCities();
    }
}
