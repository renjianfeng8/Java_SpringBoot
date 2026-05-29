package com.example.springboot.common;

import java.util.List;

public interface BaseMapper<T> {

    List<T> selectAll(T entity);

    T selectById(Integer id);

    void insert(T entity);

    void updateById(T entity);

    void deleteById(Integer id);

    void deleteBatch(List<Integer> ids);
}
