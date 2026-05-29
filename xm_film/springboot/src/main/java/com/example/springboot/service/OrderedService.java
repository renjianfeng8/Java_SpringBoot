package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Ordered;
import com.example.springboot.mapper.OrderedMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderedService extends BaseService<Ordered> {

    @Resource
    private OrderedMapper orderedMapper;

    @Override
    protected BaseMapper<Ordered> mapper() {
        return orderedMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Ordered ordered) {
        ordered.setStatus(null);
        mapper().updateById(ordered);
    }
}
