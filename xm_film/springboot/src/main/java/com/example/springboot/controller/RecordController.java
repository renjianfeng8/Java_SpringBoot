package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Record;
import com.example.springboot.service.RecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "放映记录管理", description = "排片/放映场次 CRUD")
@RestController
@RequestMapping("/api/v1/records")
public class RecordController extends BaseController<Record> {
    public RecordController(RecordService recordService) {
        super(recordService);
    }
}
