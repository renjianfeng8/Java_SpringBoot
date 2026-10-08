package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Notice;
import com.example.springboot.service.NoticeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController extends BaseController<Notice> {
    public NoticeController(NoticeService noticeService) {
        super(noticeService);
    }
}
