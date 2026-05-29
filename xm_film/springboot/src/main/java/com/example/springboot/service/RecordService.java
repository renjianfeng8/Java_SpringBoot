package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Record;
import com.example.springboot.mapper.RecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecordService extends BaseService<Record> {

    @Resource
    private RecordMapper recordMapper;

    @Override
    protected BaseMapper<Record> mapper() {
        return recordMapper;
    }
}
