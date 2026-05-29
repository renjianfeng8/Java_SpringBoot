package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Mark;
import com.example.springboot.mapper.MarkMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MarkService extends BaseService<Mark> {

    @Resource
    private MarkMapper markMapper;

    @Override
    protected BaseMapper<Mark> mapper() {
        return markMapper;
    }
}
