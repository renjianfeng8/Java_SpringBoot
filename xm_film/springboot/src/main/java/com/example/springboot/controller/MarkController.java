package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Mark;
import com.example.springboot.service.MarkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "评价管理", description = "用户评价 CRUD")
@RestController
@RequestMapping("/api/v1/marks")
public class MarkController extends BaseController<Mark> {
    public MarkController(MarkService markService) {
        super(markService);
    }
}
