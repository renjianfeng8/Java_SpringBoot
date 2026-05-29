package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Area;
import com.example.springboot.mapper.AreaMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AreaService extends BaseService<Area> {

    @Resource
    private AreaMapper areaMapper;

    @Override
    protected BaseMapper<Area> mapper() {
        return areaMapper;
    }
}
