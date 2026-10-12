package com.example.springboot.common;

import java.util.List;

public interface BaseMapper<T> {

    List<T> selectAll(T entity);

    T selectById(Integer id);

    int insert(T entity);

    int updateById(T entity);

    int deleteById(Integer id);

    int deleteBatch(List<Integer> ids);
}
