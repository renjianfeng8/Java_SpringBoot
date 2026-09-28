package com.example.springboot.mapper;

import com.example.springboot.entity.RechargeOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 充值单据不是通用 CRUD 资源：单据是资金凭证，不提供删除；
 * 状态只能经回调端点流转，故不继承 BaseMapper。
 */
public interface RechargeOrderMapper {

    void insert(RechargeOrder entity);

    List<RechargeOrder> selectAll(RechargeOrder entity);

    RechargeOrder selectById(Integer id);

    /** 回调处理前取行锁，防止并发重复回调把同一张单据入账两次 */
    RechargeOrder selectByIdForUpdate(@Param("id") Integer id);

    void updateById(RechargeOrder entity);
}
