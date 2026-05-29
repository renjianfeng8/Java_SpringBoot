package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Notice;
import com.example.springboot.mapper.NoticeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NoticeService extends BaseService<Notice> {

    @Resource
    private NoticeMapper noticeMapper;

    @Override
    protected BaseMapper<Notice> mapper() {
        return noticeMapper;
    }
}
