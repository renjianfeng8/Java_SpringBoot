package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.dto.request.AreaRequest;
import com.example.springboot.dto.response.AreaView;
import com.example.springboot.entity.Area;
import com.example.springboot.service.AreaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/areas")
public class AreaController extends BaseController<Area, AreaRequest, AreaView> {
    public AreaController(AreaService areaService) {
        super(areaService);
    }

    @Override
    protected Area toEntity(AreaRequest request) {
        Area area = new Area();
        area.setId(request.getId());
        area.setTitle(request.getTitle());
        return area;
    }

    @Override
    protected AreaView toView(Area area) {
        AreaView view = new AreaView();
        view.setId(area.getId());
        view.setTitle(area.getTitle());
        return view;
    }
}
