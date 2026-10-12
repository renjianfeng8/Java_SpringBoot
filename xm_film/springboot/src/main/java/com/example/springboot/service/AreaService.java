package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Area;
import com.example.springboot.mapper.AreaMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 区域的通用增删改查，直接复用 BaseService 模板，无额外业务规则。
 */
@Service
@Transactional(readOnly = true)
public class AreaService extends BaseService<Area> {

    private final AreaMapper areaMapper;

    public AreaService(AreaMapper areaMapper) {
        this.areaMapper = areaMapper;
    }

    @Override
    protected BaseMapper<Area> mapper() {
        return areaMapper;
    }
}
