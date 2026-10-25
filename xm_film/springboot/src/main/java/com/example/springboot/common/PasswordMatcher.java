package com.example.springboot.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 口令的一半：<b>怎么存</b>与<b>怎么比</b>。
 *
 * <p><b>为什么要有这个类</b>：{@code PasswordEncoder} 原先在 UserService、AdminService、
 * CinemaService、PayPasswordService 里各 {@code new} 一份，而"比对"这个谓词
 * （{@code BCrypt 匹配 || 明文相等}）又被抄了 4 处。这不是无所谓的重复 ——
 * 它是<b>一条安全规则</b>：只要有一处悄悄去掉了明文兼容、或把顺序反过来，
 * 三个入口之间就会口径分裂，而分裂本身不会让任何测试变红（各处仍按自己的口径正确）。
 * 收敛到这里之后，"什么算密码正确"只有一处可改。
 *
 * <h2>存量明文怎么办</h2>
 * 演示库与手工插入的旧账号可能存的是明文。启动时由
 * {@link com.example.springboot.common.config.PlaintextPasswordMigration} 统一转成 BCrypt；
 * 本类不再接受“提交值与库中值相等”这种回退 —— 那会让库里的 BCrypt 串本身变成可用密码
 * （已登记的 SEC-03）。
 *
 * <p><b>刻意不把回退分支藏起来</b>：它曾经就是一行 {@code raw.equals(stored)}，看得见，
 * 修的时候删得掉。如果为了"代码好看"把它包装成一个语义模糊的工具方法，
 * 反而会让后来者以为这是正常设计。
 */
public final class PasswordMatcher {

    /**
     * 进程内共享实例。BCrypt 编码器无状态，可以安全复用；
     * 共享也避免了 4 个 bean 各自持有不同参数（不同 strength）的隐患。
     */
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordMatcher() {}

    /** 存库前编码（BCrypt）。库里的密码列只应通过本方法写入新值。 */
    public static String encode(String raw) {
        return ENCODER.encode(raw);
    }

    /** 共享的编码器，供需要在别处直接调 {@code encode} 的调用方复用实例 */
    public static PasswordEncoder instance() {
        return ENCODER;
    }

    /**
     * 比对口令：只认 BCrypt。存量明文由启动迁移转成 BCrypt，见类注释。
     *
     * <p>空值一律视为不匹配 —— 避免 {@code raw = null} 时的 NPE，
     * 也避免"库里密码列为空 + 提交空密码"被判为通过。
     */
    public static boolean matches(PasswordEncoder encoder, String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        return encoder.matches(raw, stored);
    }
}
