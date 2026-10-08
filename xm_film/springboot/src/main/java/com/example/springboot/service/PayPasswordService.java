package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.dto.response.PayPasswordState;
import com.example.springboot.entity.User;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 支付密码的唯一读写出处，也是它的限次闸门。
 *
 * 与登录密码的分工：登录密码回答"你是谁"，支付密码回答"这笔钱你同意付"。
 * 后者每次扣款都要过，且必须限次 —— 6 位纯数字只有 10^6 种，不限次等于没有密码。
 *
 * 只有 {@code user} 表有这三列（admin / cinema 没有余额也没有支付密码），
 * 与 AccountController「仅 USER 有账户余额」是同一个前提。
 *
 * 两个 check 方法**返回异常而不抛异常**，调用方拿到非 null 自行抛出 —— 原因见方法注释，
 * 一句话：失败计数必须先提交，不能被随后的回滚带走。
 */
@Service
public class PayPasswordService {

    /** 连续错误多少次锁定 */
    private static final int MAX_ATTEMPTS = 5;

    /** 锁定时长（分钟） */
    private static final int LOCK_MINUTES = 15;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private UserMapper userMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 是否已设置支付密码；前端据此决定「引导去设置」还是「就地输密码」 */
    public boolean hasPayPassword(Integer userId) {
        return userMapper.countPayPasswordSet(userId) > 0;
    }

    /**
     * 支付前校验。通过则清零计数，失败则递增并在触顶时锁定。
     *
     * **返回「该抛的异常」而不是自己抛，也不是随手可改的写法。** 校验失败必须把错误次数
     * 写进库，而"写库"与"抛异常"如果在同一个事务里，异常会把刚写的计数一起回滚，
     * 错误次数永远记不下来、限次形同虚设 —— REQUIRES_NEW 也救不了这种自己抛自己回滚。
     * 所以这里的写法是：本方法在自己那个 REQUIRES_NEW 事务里写库、**正常返回**让它提交，
     * 由调用方在它自己的事务里抛（Bug.md 规则 29）。
     *
     * @return {@code null} 表示校验通过；否则是调用方应当抛出的异常（错误码与提示都在里面）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CustomException checkForPayment(Integer userId, String rawPassword) {
        return check(userId, rawPassword, "支付密码");
    }

    /**
     * 修改支付密码前的原码校验，语义同 {@link #checkForPayment}。
     * 与支付校验共用同一个方法、同一套计数与锁定 —— 否则"改密码"就是一条不限次的
     * 试错通道，限次可以绕过去。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CustomException checkOldPassword(Integer userId, String rawOldPassword) {
        return check(userId, rawOldPassword, "原支付密码");
    }

    /** 修改支付密码：验原码（含限次）后写入新码 */
    public void changePayPassword(Integer userId, String rawOldPassword, String newPayPassword) {
        CustomException failure = checkOldPassword(userId, rawOldPassword);
        if (failure != null) {
            throw failure;
        }
        userMapper.updatePayPassword(userId, passwordEncoder.encode(newPayPassword));
    }

    /**
     * 设置 / 重设支付密码：验登录密码后写入。
     *
     * 登录密码是本系统的根凭证（登录与改密都只认它），所以"忘了支付密码"用它可以自救；
     * 这条路径验的不是支付密码，因此不共享计数与锁定。
     * 登录密码的判定与 {@code UserService.login} / {@code updatePassword} 同口径：
     * 先 BCrypt 比对，再退到明文相等（存量种子账号的密码是明文，见 data.sql）。
     */
    public void resetWithLoginPassword(Integer userId, String rawLoginPassword, String newPayPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "账号不存在");
        }
        String stored = user.getPassword();
        if (!passwordEncoder.matches(rawLoginPassword, stored) && !stored.equals(rawLoginPassword)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "登录密码错误");
        }
        userMapper.updatePayPassword(userId, passwordEncoder.encode(newPayPassword));
    }

    /** 校验本体。全程不抛异常：要么返回 null（通过），要么返回调用方该抛的那个异常 */
    private CustomException check(Integer userId, String rawPassword, String label) {
        PayPasswordState state = userMapper.selectPayPasswordState(userId);
        if (state == null) {
            return new CustomException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (state.getPayPassword() == null) {
            return new CustomException(ErrorCode.BUSINESS_CONFLICT, "尚未设置支付密码，请先设置后再支付");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        LocalDateTime lockedUntil = RecordService.readStart(state.getLockedUntil());
        if (lockedUntil != null && now.isBefore(lockedUntil)) {
            return new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "支付密码已锁定，请 " + remainingMinutes(now, lockedUntil) + " 分钟后再试");
        }
        // 锁定期已过：先抹掉残留的计数与锁定，再按未锁定继续本次校验
        if (lockedUntil != null) {
            userMapper.clearPayPasswordFailure(userId);
        }

        if (passwordEncoder.matches(rawPassword, state.getPayPassword())) {
            userMapper.clearPayPasswordFailure(userId);
            return null;
        }

        userMapper.incrementPayPasswordFailure(userId, MAX_ATTEMPTS, lockDeadline());
        // 读回真值再拼提示：并发尝试下递增是原子的，用本地推算出来的次数可能偏低
        Integer errorCount = userMapper.selectPayPasswordErrorCount(userId);
        int failed = errorCount == null ? MAX_ATTEMPTS : errorCount;
        if (failed >= MAX_ATTEMPTS) {
            return new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "支付密码连续输错 " + MAX_ATTEMPTS + " 次，已锁定 " + LOCK_MINUTES + " 分钟");
        }
        return new CustomException(ErrorCode.UNAUTHORIZED,
                label + "错误，还可重试 " + (MAX_ATTEMPTS - failed) + " 次");
    }

    /** 向上取整到分钟：剩 30 秒也提示"1 分钟"，说"0 分钟"会让人以为已经能试了 */
    private static long remainingMinutes(LocalDateTime now, LocalDateTime lockedUntil) {
        long seconds = Duration.between(now, lockedUntil).getSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }

    private static String lockDeadline() {
        return LocalDateTime.now(ZoneId.systemDefault())
                .plusMinutes(LOCK_MINUTES)
                .format(DATE_TIME_FORMATTER);
    }
}
