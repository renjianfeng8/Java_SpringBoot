package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Notice;
import com.example.springboot.mapper.NoticeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 公告的通用增删改查，直接复用 BaseService 模板，无额外业务规则。
 */
@Service
@Transactional(readOnly = true)
public class NoticeService extends BaseService<Notice> {

    private final NoticeMapper noticeMapper;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    @Override
    protected BaseMapper<Notice> mapper() {
        return noticeMapper;
    }
}
