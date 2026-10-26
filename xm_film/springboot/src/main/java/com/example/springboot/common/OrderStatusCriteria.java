package com.example.springboot.common;

import com.example.springboot.common.enums.CinemaStatus;
import com.example.springboot.common.enums.OrderStatus;
import com.example.springboot.common.enums.RechargeStatus;
import com.example.springboot.common.enums.RecordStatus;

/**
 * Mapper 里"状态取值"的唯一来源：把状态常量带进 SQL，而不是在 XML 里写中文字面量。
 *
 * <h2>它解决什么问题</h2>
 * 状态列是 {@code varchar(20)}，库层不会拒绝任何字符串。于是 XML 里的
 * {@code status = '待取票'} 与 Java 里的 {@code OrderStatus.PENDING} 是<b>两份副本</b>：
 * 改常量名或改取值时编译器不报错、数据库也不报错，只是那条 SQL 从此匹配到 0 行。
 * 本类让本文件成为唯一出处，XML 只写 {@code #{s.pending}}。
 *
 * <h2>为什么是"实例 + @Param("s")"而不是静态方法</h2>
 * MyBatis 的 {@code #{s.pending}} 走 getter 取值，所以这里用 {@code getXxx()} 形式的
 * 实例方法；{@code INSTANCE} 是不可变单例，所有 mapper 方法共用，不引入状态。
 *
 * <h2>为什么 {@code paidOrderStatuses} 片段没有纳入</h2>
 * 那个片段被 {@code FilmMapper.filmRevenueJoin} <b>跨 namespace 静态引用</b>
 * （{@code <include refid="...OrderedMapper.paidOrderStatuses"/>}），
 * 而 {@code FilmMapper} 的 8 个查询方法都只是 {@code (Integer topNum)}、
 * 没有这个参数。要纳进来就得给全部 8 个方法加参数，收益低于成本。
 * 它的字面量副本改由 {@code StatusLiteralGuardTest} 钉住"与枚举逐字一致"。
 */
@SuppressWarnings("java:S6548") // 无状态共享单例，MyBatis 只读 getter
public final class OrderStatusCriteria {

    /** 无状态的共享实例（MyBatis 只读它的 getter） */
    public static final OrderStatusCriteria INSTANCE = new OrderStatusCriteria();

    private OrderStatusCriteria() {}

    public String getPendingPayment() {
        return OrderStatus.PENDING_PAYMENT;
    }

    public String getPending() {
        return OrderStatus.PENDING;
    }

    public String getPickedUp() {
        return OrderStatus.PICKED_UP;
    }

    public String getCancelled() {
        return OrderStatus.CANCELLED;
    }

    public String getRefunded() {
        return OrderStatus.REFUNDED;
    }

    /* 以下为其他实体的状态，XML 里同样存在字面量副本；一并从这里取，便于守卫测试统一核对。 */

    public String getRecordNormal() {
        return RecordStatus.NORMAL;
    }

    public String getRecordStopped() {
        return RecordStatus.STOPPED;
    }

    public String getCinemaApproved() {
        return CinemaStatus.APPROVED;
    }

    public String getCinemaUnaudited() {
        return CinemaStatus.UNAUDITED;
    }

    public String getRechargeProcessing() {
        return RechargeStatus.PROCESSING;
    }

    public String getRechargeDone() {
        return RechargeStatus.SUCCESS;
    }

    public String getRechargeFailed() {
        return RechargeStatus.FAILED;
    }
}
