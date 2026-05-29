package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Mark;
import com.example.springboot.service.MarkService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/marks")
public class MarkController extends BaseController<Mark> {
    public MarkController(MarkService markService) {
        super(markService);
    }
}
