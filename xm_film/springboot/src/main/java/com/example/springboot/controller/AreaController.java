package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Area;
import com.example.springboot.service.AreaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "地区管理", description = "电影产地/区域 CRUD")
@RestController
@RequestMapping("/api/v1/areas")
public class AreaController extends BaseController<Area> {
    public AreaController(AreaService areaService) {
        super(areaService);
    }
}
