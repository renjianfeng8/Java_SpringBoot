package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Record;
import com.example.springboot.service.RecordService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/records")
public class RecordController extends BaseController<Record> {
    public RecordController(RecordService recordService) {
        super(recordService);
    }
}
