package com.example.springboot.mapper;

import com.example.springboot.entity.FundFlow;

import java.util.List;

/** 资金流水是只增账本：只有写入与查询，没有更新与删除。 */
public interface FundFlowMapper {

    void insert(FundFlow entity);

    List<FundFlow> selectAll(FundFlow entity);
}
