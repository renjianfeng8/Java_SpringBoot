package com.example.springboot.mapper;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.dto.response.PayPasswordState;
import com.example.springboot.entity.User;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface UserMapper extends BaseMapper<User> {

    User selectByUsername(String username);

    void updatePassword(User user);

    /** 大盘：注册用户总数 */
    int countAll();

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

    /* 支付密码：与余额同理不挂 User 实体，避免 selectAll 的 SELECT * 把密文带出去。
       五个方法的唯一调用方是 PayPasswordService。 */

    /** 是否已设置支付密码（展示用，不加锁） */
    int countPayPasswordSet(@Param("id") Integer id);

    /** 校验所需的状态：密文 + 连续错误次数 + 锁定截止时刻 */
    PayPasswordState selectPayPasswordState(@Param("id") Integer id);

    /** 读回当前错误次数，用于拼「还可重试 N 次」提示 */
    Integer selectPayPasswordErrorCount(@Param("id") Integer id);

    /** 写入支付密码并清零计数与锁定（设置 / 修改 / 重设共用唯一写入口） */
    int updatePayPassword(@Param("id") Integer id, @Param("payPassword") String payPassword);

    /**
     * 原子记一次校验失败：自增计数，并在触顶时于同一条语句写入锁定截止。
     * 不是「读计数 → 算 → 写回」——那样并发尝试会互相覆盖（读到 4 的线程都写成 5），
     * 6 位密码可被并行暴破绕过限次。
     */
    int incrementPayPasswordFailure(@Param("id") Integer id,
                                    @Param("maxAttempts") int maxAttempts,
                                    @Param("lockedUntil") String lockedUntil);

    /** 校验通过或锁定到期：清零计数与锁定 */
    int clearPayPasswordFailure(@Param("id") Integer id);
}
