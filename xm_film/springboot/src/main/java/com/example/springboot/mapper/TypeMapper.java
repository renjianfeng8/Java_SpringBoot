package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.Type;

import java.util.List;

public interface TypeMapper extends BaseMapper<Type> {

    List<Type> selectByIds(List<Integer> ids);

}
