package com.example.springboot.common;

import java.security.SecureRandom;

/**
 * 凭证类随机串的唯一出处：取票码、批量导入影院的初始口令都从这里取。
 *
 * <p><b>为什么必须是 {@link SecureRandom}</b>：这两类串都是「持有即可用」的凭证 ——
 * 取票码可在匿名核销接口兑换电影票，初始口令可登录影院后台。
 * {@code ThreadLocalRandom} 只保证统计分布，内部种子可预测，不能用于凭证；
 * 字母表长度与位数只决定枚举空间，不能弥补随机源可预测的问题。
 *
 * <p>字母表由调用方决定：取票码要剔除人工抄写易混的字符，口令则用大小写字母 + 数字。
 * 本类只负责「从给定字母表里等概率取 length 个字符」，不掺业务规则。
 */
public final class SecureCodes {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureCodes() {}

    /**
     * 生成 {@code length} 位随机串。
     *
     * @param alphabet 可选字符集，至少 2 个字符
     * @param length   长度，至少 1
     */
    public static String of(String alphabet, int length) {
        if (alphabet == null || alphabet.length() < 2) {
            throw new IllegalArgumentException("字母表至少需要 2 个字符");
        }
        if (length < 1) {
            throw new IllegalArgumentException("随机串长度必须大于 0");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
