package com.example.springboot.service;

import com.example.springboot.entity.Type;
import com.example.springboot.mapper.TypeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class TypeService {

    @Resource
    private TypeMapper typeMapper;

    public List<Type> selectAll(Type type) {
        return typeMapper.selectAll(type);
    }

    public Type selectById(Integer id) {
        return typeMapper.selectById(id);

    }

    public List<Type> selectList(Type type) {
        return typeMapper.selectAll(type);
    }

    public PageInfo<Type> selectPage( Type type, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Type> list = typeMapper.selectAll(type);
        return PageInfo.of(list);
    }

    public List<Type> selectByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return typeMapper.selectByIds(ids);
    }

    public void add(Type type) {
        typeMapper.insert(type);
    }

    public void update(Type type) {
        typeMapper.updateById(type);
    }

    public void delete(Integer id) {
        typeMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.delete(id);
        }
    }

}
