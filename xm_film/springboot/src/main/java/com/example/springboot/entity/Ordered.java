package com.example.springboot.entity;

import com.example.springboot.common.Identifiable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ordered implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String orders;
    private Integer recordId;
    private Integer userId;
    private Integer filmId;
    private String img;
    private Integer cinemaId;
    private Integer roomId;
    private String appointment;
    /**
     * 订单总金额（元）。必须用包装类型：updateById 里是 <if test="total != null">，
     * 原始类型 double 永远非 null，导致任何不带 total 的状态流转（支付/取票/取消）
     * 都会把订单金额写成 0.00。
     *
     * <p><b>为什么是 BigDecimal 而不是 Double</b>（BL-24）：这是钱。java.math.BigDecimal 的
     * 类型本身就向读者声明"这里不做二进制浮点运算"；用 Double 时每个读者都得自己在心里确认
     * "哦，它只是往返，没有累加"。库列也确实是 {@code decimal(10,2)}，
     * 用 BigDecimal 之后 Java 类型与库类型一一对应，不再靠一次隐式转换维持正确性
     * （实测 decimal(10,2) 范围内 Double 往返无损，但那是巧合，不是保证）。
     * 与同实体的 {@link #unitPrice}、充值单 {@code amount}、资金流水各额口径一致。
     */
    private BigDecimal total;
    /** 下单时的单价快照，取自场次票价；场次改价不影响历史订单 */
    private BigDecimal unitPrice;
    private Integer number;
    private String status;
    private String start;
    private String seat;
    private String createTime;
    private String pendingTimeoutAt;
    private String payTime;
    /** 实付金额（元）。见 {@link #total} 的说明 */
    private BigDecimal payAmount;
    private String refundTime;
    /** 退款金额（元）。见 {@link #total} 的说明 */
    private BigDecimal refundAmount;
    /**
     * 取票码（支付成功时生成，一单一码）。可用性派生自 status，本身不带有效/失效标记：
     * 核销只接受 待取票，因此「用过即废 / 退票作废 / 未付款不出发」都由状态迁移保证。
     * 有效期到放映结束（start + 片长），同样不落库。
     */
    private String pickupCode;

    /**
     * 更新哨兵（非数据库列）：置真时 updateById 把 pending_timeout_at 显式写为 NULL。
     * 状态流转离开「待支付」（支付/取消/退票/取票）时由服务层置真 —— 倒计时随之作废。
     * 不能用 pendingTimeoutAt = null 表达"清空"：null 同时是"本次更新不涉及该列"，
     * 而部分更新绝不能被当成清空（否则自动取消扫不到该单，座位永久占死）。
     */
    private Boolean clearPendingTimeout;

    /**
     * 写入哨兵（非数据库列）：置真时 insert 用<b>数据库时钟</b>写
     * {@code pending_timeout_at = NOW() + INTERVAL 5 MINUTE}，忽略
     * {@link #pendingTimeoutAt} 里的 JVM 时间（BL-05）。
     *
     * <p>为什么必须由库算：这一列只在 SQL 里与 {@code NOW()} 比较
     * （{@code seatActiveCondition} 的占座判定、{@code selectExpiredPendingOrders} 的清理）。
     * 若写入用 JVM 时钟，JVM 与 MySQL 时区不一致时（典型：容器默认 UTC、
     * 连接串写死 Asia/Shanghai），两者相差整整 8 小时 —— 清理任务要么永不命中
     * （废单永久占座），要么提前 8 小时把有效订单取消。
     *
     * <p>超时分钟数写死在 SQL 里（5 分钟），与服务层常量 {@code PENDING_TIMEOUT_MINUTES}
     * 必须一致；改动时两处同改。
     */
    private Boolean pendingTimeoutFromDb;

    private String userName;
    private String filmName;
    private String cinemaName;
    private String roomName;
}
