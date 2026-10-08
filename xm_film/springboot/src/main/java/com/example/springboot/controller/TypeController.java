package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Type;
import com.example.springboot.service.TypeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/types")
public class TypeController extends BaseController<Type> {
    public TypeController(TypeService typeService) {
        super(typeService);
    }
}
