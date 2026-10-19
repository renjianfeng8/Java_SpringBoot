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
 * <h2>明文兼容为什么还留着</h2>
 * 存量种子账号（演示数据、手工插入的账号）的密码列存的是明文。直接删掉兼容分支会让
 * 这些账号全部登录失败。这条回退是<b>已登记的安全缺陷 SEC-03</b>
 * （{@code audit/backend-backlog.md} §安全）：它允许"库里是什么，提交什么就通过"，
 * 一旦库被读走，密码即等于明文。修复方向是迁移脚本把存量明文全部
 * BCrypt 化、然后删掉回退分支 —— 那是独立一轮的工作，本轮只做收敛、<b>不改变行为</b>。
 *
 * <p><b>刻意不把回退分支藏起来</b>：它就是一行 {@code raw.equals(stored)}，看得见，
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
     * 比对口令：先 BCrypt 匹配，不中再退到明文相等（存量账号，见类注释）。
     *
     * <p>空值一律视为不匹配 —— 避免 {@code raw = null} 时的 NPE，
     * 也避免"库里密码列为空 + 提交空密码"被判为通过。
     */
    public static boolean matches(PasswordEncoder encoder, String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        return encoder.matches(raw, stored) || raw.equals(stored);
    }
}
