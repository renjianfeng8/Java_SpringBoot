package com.example.springboot.common;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public abstract class BaseService<T extends Identifiable> {

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

    /**
     * 更新前先判存在：主键缺失或库中无此行时抛 NOT_FOUND，而不是让 updateById 影响 0 行后
     * 静默返回成功。此处是 update 存在性校验的唯一出处——覆写 update 的子类须委派到本方法，
     * 不要各自再造一份（口径分裂后，陈旧表单提交会在一部分实体上得 200、另一部分得 404）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(T entity) {
        if (entity.getId() == null || mapper().selectById(entity.getId()) == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "记录不存在，可能已被删除");
        }
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
