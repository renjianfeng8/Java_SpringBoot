package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.dto.request.NoticeRequest;
import com.example.springboot.dto.response.NoticeView;
import com.example.springboot.entity.Notice;
import com.example.springboot.service.NoticeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController extends BaseController<Notice, NoticeRequest, NoticeView> {
    public NoticeController(NoticeService noticeService) {
        super(noticeService);
    }

    @Override
    protected Notice toEntity(NoticeRequest request) {
        Notice notice = new Notice();
        notice.setId(request.getId());
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setTime(request.getTime());
        return notice;
    }

    @Override
    protected NoticeView toView(Notice notice) {
        NoticeView view = new NoticeView();
        view.setId(notice.getId());
        view.setTitle(notice.getTitle());
        view.setContent(notice.getContent());
        view.setTime(notice.getTime());
        return view;
    }
}
