package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.entity.User;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface UserMapper extends BaseMapper<User> {

    User selectByUsername(String username);

    void updatePassword(User user);

    /* 账户余额：余额不挂 User 实体，避免 selectAll 的 SELECT * 把他人余额带出去 */

    /** 取余额并加行锁，供「读余额 → 变更 → 记流水」在同一事务内串行化 */
    BigDecimal selectBalanceForUpdate(@Param("id") Integer id);

    /** 不加锁的余额查询，仅供展示（账户摘要） */
    BigDecimal selectBalance(@Param("id") Integer id);

    /**
     * 扣减余额。带 balance >= #{amount} 守卫：并发下扣不动的调用影响行数为 0，
     * 由调用方翻译成「余额不足」，余额不会变成负数。
     */
    int deductBalance(@Param("id") Integer id, @Param("amount") BigDecimal amount);

    /** 增加余额（充值入账 / 退票退款） */
    int addBalance(@Param("id") Integer id, @Param("amount") BigDecimal amount);
}
