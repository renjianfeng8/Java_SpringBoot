package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Type;
import com.example.springboot.service.TypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "类型管理", description = "电影分类 CRUD")
@RestController
@RequestMapping("/api/v1/types")
public class TypeController extends BaseController<Type> {
    public TypeController(TypeService typeService) {
        super(typeService);
    }
}
