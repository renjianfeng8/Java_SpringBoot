package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Area;
import com.example.springboot.service.AreaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/areas")
public class AreaController extends BaseController<Area> {
    public AreaController(AreaService areaService) {
        super(areaService);
    }
}
