package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.dto.request.TypeRequest;
import com.example.springboot.dto.response.TypeView;
import com.example.springboot.entity.Type;
import com.example.springboot.service.TypeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/types")
public class TypeController extends BaseController<Type, TypeRequest, TypeView> {
    public TypeController(TypeService typeService) {
        super(typeService);
    }

    @Override
    protected Type toEntity(TypeRequest request) {
        Type type = new Type();
        type.setId(request.getId());
        type.setTitle(request.getTitle());
        return type;
    }

    @Override
    protected TypeView toView(Type type) {
        TypeView view = new TypeView();
        view.setId(type.getId());
        view.setTitle(type.getTitle());
        return view;
    }
}
