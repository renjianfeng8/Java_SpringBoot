package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Notice;
import com.example.springboot.service.NoticeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "公告管理", description = "系统公告 CRUD")
@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController extends BaseController<Notice> {
    public NoticeController(NoticeService noticeService) {
        super(noticeService);
    }
}
