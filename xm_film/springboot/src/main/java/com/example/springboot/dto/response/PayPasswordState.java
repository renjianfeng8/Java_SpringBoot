package com.example.springboot.dto.response;

import lombok.Data;

/**
 * 支付密码的校验状态（含密文）。
 *
 * 与登录密码、余额同样不挂在 {@code User} 实体上：这三列都是 {@code SELECT *}
 * 会顺手带出去的敏感列，实体上加字段等于把它们塞进任意一处用户列表响应。
 * 全仓只有 PayPasswordService 读它。
 */
@Data
public class PayPasswordState {

    /**
     * 主键。查询里必须带上它：MyBatis 默认把"映射出来全是 null"的行当作没有行返回 null
     * （returnInstanceForEmptyRow 是全局 setting，不能按语句关）。未设置支付密码的用户
     * 恰好只有这一列非空，带上它才能把「用户不存在」与「用户存在但没设过支付密码」分开。
     */
    private Integer id;

    /** BCrypt 密文；null 表示尚未设置支付密码 */
    private String payPassword;

    /** 连续错误次数 */
    private Integer errorCount;

    /** 锁定截止时刻（"yyyy-MM-dd HH:mm:ss"）；null 表示未锁定 */
    private String lockedUntil;
}
