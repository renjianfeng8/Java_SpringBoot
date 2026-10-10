package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Type;
import com.example.springboot.mapper.TypeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 影片类型的增删改查，外加按 id 批量取值；空入参直接返回空表，不下发 {@code IN ()}。
 */
@Service
@Transactional(readOnly = true)
public class TypeService extends BaseService<Type> {

    @Resource
    private TypeMapper typeMapper;

    @Override
    protected BaseMapper<Type> mapper() {
        return typeMapper;
    }

    public List<Type> selectByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return typeMapper.selectByIds(ids);
    }
}
