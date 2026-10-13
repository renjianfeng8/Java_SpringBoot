package com.example.springboot.common;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public abstract class BaseService<T> {

    protected abstract BaseMapper<T> mapper();

    public List<T> selectAll(T entity) {
        return mapper().selectAll(entity);
    }

    public T selectById(Integer id) {
        return mapper().selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(T entity) {
        mapper().insert(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(T entity) {
        mapper().updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        mapper().deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Integer> ids) {
        // 空集合不能落到 mapper：<foreach> 会拼出 `IN ()`，MySQL 报语法错。
        // 删零条即无操作，直接返回。
        if (ids == null || ids.isEmpty()) {
            return;
        }
        mapper().deleteBatch(ids);
    }
}
