package com.example.springboot.common;

/**
 * 业务成功码的唯一出处（响应体 {@code code} 字段）。
 *
 * <p><b>为什么单独抽一个类，而不是写进 {@code ErrorCode}</b>：{@code ErrorCode} 枚举的是
 * <b>错误</b>，成功不是错误的一种；把它塞进去会让 {@code ErrorCode.values()} 里混进一个
 * 永远不该被当作"失败"抛出的成员。但两者又必须放在一起看 —— 因为它们共同构成
 * "响应体 code 字段的取值范围"，而且前端也有一份同值常量
 * （{@code vue/src/constants/index.js} 的 {@code SUCCESS_CODE}），
 * 这两份必须一致，改动时两边都要动。
 *
 * <p><b>为什么是 String 而不是 int</b>：这是既有契约，前端按字符串比较。
 * 本类不提供 {@code int} 版本，避免有人 {@code == 200} 时类型静默不匹配。
 */
public final class SuccessCode {

    /** 成功：字符串 {@code "200"}。与 HTTP 状态码无关，只在响应体里使用。 */
    public static final String VALUE = "200";

    private SuccessCode() {}
}
