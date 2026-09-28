package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.OrderStatus;
import com.example.springboot.common.enums.PayResult;
import com.example.springboot.common.enums.RecordStatus;
import com.example.springboot.dto.response.SeatOccupancy;
import com.example.springboot.entity.Film;
import com.example.springboot.entity.Ordered;
import com.example.springboot.entity.Record;
import com.example.springboot.entity.Room;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.mapper.RoomMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderedService extends BaseService<Ordered> {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 座位号形状：{行}排{列}座；合法范围由所属影厅的座位行列数决定，不再写死 8×8 */
    private static final Pattern SEAT_SHAPE = Pattern.compile("(\\d{1,2})排(\\d{1,2})座");

    /** 影厅座位数缺失时的兜底，与 room 表列默认值一致 */
    private static final int DEFAULT_SEAT_ROWS = 8;
    private static final int DEFAULT_SEAT_COLS = 8;

    /** 单笔订单最多选择的座位数，防止恶意一次性锁满整个影厅 */
    private static final int MAX_SEATS_PER_ORDER = 6;

    /** 退票截止：放映前 60 分钟（与前台词：未取票用户在放映前60分钟可退票） */
    private static final int REFUND_DEADLINE_MINUTES = 60;

    /**
     * 只有终态废单可以清理。已成交订单必须走退票流程，
     * 否则「删除订单」会成为绕过退票与资金凭证的后门。
     */
    private static final Set<String> DELETABLE_STATUSES =
            Set.of(OrderStatus.CANCELLED, OrderStatus.REFUNDED);

    @Resource
    private OrderedMapper orderedMapper;

    @Resource
    private RecordMapper recordMapper;

    @Resource
    private FilmMapper filmMapper;

    @Resource
    private RoomMapper roomMapper;

    @Resource
    private WalletService walletService;

    @Override
    protected BaseMapper<Ordered> mapper() {
        return orderedMapper;
    }

    public void applyScope(Ordered ordered, String role, Integer userId) {
        if (ordered == null || role == null || userId == null) {
            return;
        }
        if ("USER".equals(role)) {
            ordered.setUserId(userId);
        } else if ("CINEMA".equals(role)) {
            ordered.setCinemaId(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Ordered ordered) {
        insertOrder(ordered, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Ordered ordered, String role, Integer tokenUserId) {
        insertOrder(ordered, role, tokenUserId);
    }

    private void insertOrder(Ordered ordered, String role, Integer tokenUserId) {
        if (role != null && !"USER".equals(role)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "仅普通用户可创建订单");
        }
        if (ordered == null || ordered.getRecordId() == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "缺少放映场次");
        }
        if (tokenUserId != null) {
            ordered.setUserId(tokenUserId);
        }
        if (ordered.getUserId() == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "缺少购票用户");
        }

        Record recordItem = recordMapper.selectByIdForUpdate(ordered.getRecordId());
        if (recordItem == null) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "放映场次不存在");
        }
        if (RecordStatus.STOPPED.equals(recordItem.getStatus())) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "该场次已停售");
        }
        if (!RecordService.isPurchasable(recordItem)) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "该场次已开场，无法购票");
        }

        List<String> seats = normalizeSeats(ordered.getSeat(), recordItem.getRoomId());
        int number = seats.size();
        for (String seat : seats) {
            if (orderedMapper.countSeatInUse(recordItem.getId(), seat) > 0) {
                throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "座位已售: " + seat);
            }
        }

        Film film = filmMapper.selectById(recordItem.getFilmId());
        BigDecimal price = parsePrice(recordItem.getPrice());
        BigDecimal total = price.multiply(BigDecimal.valueOf(number)).setScale(2, RoundingMode.HALF_UP);

        ordered.setOrders(generateOrderNo());
        ordered.setRecordId(recordItem.getId());
        ordered.setFilmId(recordItem.getFilmId());
        ordered.setCinemaId(recordItem.getCinemaId());
        ordered.setRoomId(recordItem.getRoomId());
        ordered.setAppointment("场次ID:" + recordItem.getId());
        ordered.setStart(recordItem.getStart());
        ordered.setNumber(number);
        ordered.setTotal(total.doubleValue());
        // 单价快照：场次日后改价不影响这张订单的金额还原
        ordered.setUnitPrice(price);
        ordered.setStatus(OrderStatus.PENDING_PAYMENT);
        ordered.setSeat(String.join(",", seats));
        ordered.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        ordered.setPendingTimeoutAt(LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(5).format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        if (film != null) {
            ordered.setImg(film.getImg());
        }

        orderedMapper.insert(ordered);
    }

    /**
     * 支付待支付订单。
     *
     * 超时分支不能抛异常返回：本方法标注了 rollbackFor = Exception.class，
     * 抛异常会把"把订单置为已取消"这次写入一起回滚，订单又变回待支付。
     * 因此超时改为返回 {@link PayResult#TIMEOUT_CANCELLED}，由控制器映射为业务错误。
     */
    @Transactional(rollbackFor = Exception.class)
    public PayResult payOrder(Integer id, String role, Integer userId) {
        Ordered ordered = orderedMapper.selectByIdForUpdate(id);
        if (ordered == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (!OrderStatus.PENDING_PAYMENT.equals(ordered.getStatus())) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "当前状态不允许支付");
        }
        ensureOrderAccess(ordered, role, userId);

        if (isPendingTimedOut(ordered)) {
            Ordered cancel = new Ordered();
            cancel.setId(id);
            cancel.setStatus(OrderStatus.CANCELLED);
            cancel.setPendingTimeoutAt(null);
            orderedMapper.updateById(cancel);
            return PayResult.TIMEOUT_CANCELLED;
        }

        // 余额校验与扣减必须与出票在同一事务内：扣款失败（余额不足）整笔回滚，
        // 订单停在待支付、pending_timeout_at 不变、座位继续锁定，用户充值后可继续支付。
        walletService.debitPurchase(ordered.getUserId(), orderAmount(ordered), id);

        Ordered update = new Ordered();
        update.setId(id);
        update.setStatus(OrderStatus.PENDING);
        update.setPendingTimeoutAt(null);
        update.setPayTime(now());
        update.setPayAmount(ordered.getTotal());
        orderedMapper.updateById(update);
        return PayResult.PAID;
    }

    /**
     * 退票：仅待取票（即已支付未取票）订单可退，且需在放映前 60 分钟之前办理。
     * 退票后订单流转为已退票，座位不再计入占用（见 OrderedMapper.countSeatInUse）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Integer id, String role, Integer userId) {
        Ordered ordered = orderedMapper.selectByIdForUpdate(id);
        ensureOrderAccess(ordered, role, userId);
        if (!OrderStatus.PENDING.equals(ordered.getStatus())) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "仅待取票订单可退票");
        }

        LocalDateTime start = RecordService.readStart(ordered.getStart());
        if (start == null) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "订单缺失放映时间，无法退票，请联系影院");
        }
        if (LocalDateTime.now(ZoneId.systemDefault()).isAfter(start.minusMinutes(REFUND_DEADLINE_MINUTES))) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "已过退票截止时间（放映前 " + REFUND_DEADLINE_MINUTES + " 分钟）");
        }

        // 退款入账与状态流转同事务：入账成功但状态没改回去的情况不会出现
        walletService.creditRefund(ordered.getUserId(), orderAmount(ordered), id);

        Ordered update = new Ordered();
        update.setId(id);
        update.setStatus(OrderStatus.REFUNDED);
        update.setRefundTime(now());
        update.setRefundAmount(ordered.getTotal());
        update.setPendingTimeoutAt(null);
        orderedMapper.updateById(update);
    }

    private boolean isPendingTimedOut(Ordered ordered) {
        LocalDateTime timeout = RecordService.readStart(ordered.getPendingTimeoutAt());
        return timeout != null && LocalDateTime.now(ZoneId.systemDefault()).isAfter(timeout);
    }

    private String now() {
        return LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Ordered ordered) {
        Ordered db = mapper().selectById(ordered.getId());
        if (db != null) {
            ordered.setPendingTimeoutAt(db.getPendingTimeoutAt());
        }
        ordered.setStatus(null);
        mapper().updateById(ordered);
    }

    /**
     * 选座图视角：只给座位与归属，本人订单附带继续支付所需的字段。
     *
     * 不能直接把 {@code Ordered} 实体发给前端 —— 那等于把该场次所有订单的
     * 订单号、用户 ID、金额都给到任意登录用户。归属判定只看 JWT 里的 userId。
     */
    public List<SeatOccupancy> selectSeatOccupancy(Integer recordId, Integer tokenUserId) {
        return orderedMapper.selectActiveByRecordId(recordId).stream()
                .map(ordered -> toSeatOccupancy(ordered, tokenUserId))
                .toList();
    }

    private SeatOccupancy toSeatOccupancy(Ordered ordered, Integer tokenUserId) {
        SeatOccupancy view = new SeatOccupancy();
        view.setSeat(ordered.getSeat());
        boolean mine = tokenUserId != null && tokenUserId.equals(ordered.getUserId());
        view.setMine(mine);
        if (!mine) {
            return view;
        }
        view.setOrderId(ordered.getId());
        view.setOrders(ordered.getOrders());
        view.setStatus(ordered.getStatus());
        view.setTotal(ordered.getTotal());
        view.setPendingTimeoutAt(ordered.getPendingTimeoutAt());
        return view;
    }

    public int countByFilmId(Integer filmId) {
        return orderedMapper.countByFilmId(filmId);
    }

    public int countByCinemaId(Integer cinemaId) {
        return orderedMapper.countByCinemaId(cinemaId);
    }

    public int countByRoomId(Integer roomId) {
        return orderedMapper.countByRoomId(roomId);
    }

    public int countByRecordId(Integer recordId) {
        return orderedMapper.countByRecordId(recordId);
    }

    public int countByUserId(Integer userId) {
        return orderedMapper.countByUserId(userId);
    }

    public Ordered selectByIdScoped(Integer id, String role, Integer userId) {
        Ordered ordered = selectById(id);
        ensureOrderAccess(ordered, role, userId);
        return ordered;
    }

    public void updateScoped(Ordered ordered, String role, Integer userId) {
        throw new CustomException(
                ErrorCode.FORBIDDEN,
                "订单不支持通用更新，请使用取消/支付/取票/退票显式接口"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteScoped(Integer id, String role, Integer userId) {
        Ordered ordered = selectById(id);
        ensureOrderAccess(ordered, role, userId);
        ensureDeletable(ordered);
        orderedMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchScoped(List<Integer> ids, String role, Integer userId) {
        if (ids == null || ids.isEmpty()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择订单");
        }
        for (Integer id : ids) {
            Ordered ordered = selectById(id);
            ensureOrderAccess(ordered, role, userId);
            ensureDeletable(ordered);
        }
        orderedMapper.deleteBatch(ids);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Integer id, String role, Integer userId) {
        Ordered ordered = orderedMapper.selectByIdForUpdate(id);
        ensureOrderAccess(ordered, role, userId);
        if (!OrderStatus.PENDING_PAYMENT.equals(ordered.getStatus())) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "当前状态不允许取消订单");
        }
        Ordered update = new Ordered();
        update.setId(id);
        update.setStatus(OrderStatus.CANCELLED);
        update.setPendingTimeoutAt(null);
        orderedMapper.updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    public void pickupOrder(Integer id, String role, Integer userId) {
        Ordered ordered = orderedMapper.selectByIdForUpdate(id);
        ensureOrderAccess(ordered, role, userId);
        if ("USER".equals(role)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "用户无权执行取票操作");
        }
        if (!OrderStatus.PENDING.equals(ordered.getStatus())) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT, "当前状态不允许取票");
        }
        Ordered update = new Ordered();
        update.setId(id);
        update.setStatus(OrderStatus.PICKED_UP);
        orderedMapper.updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredPendingOrders() {
        List<Ordered> expired = orderedMapper.selectExpiredPendingOrders();
        if (expired.isEmpty()) {
            return;
        }
        List<Integer> ids = expired.stream().map(Ordered::getId).toList();
        orderedMapper.batchCancelExpiredOrders(ids);
    }

    /**
     * 删除守卫：只有「已取消 / 已退票」的终态废单可以物理删除。
     * 待支付/待取票/已取票订单一律拒绝，把「删订单当免费退票用」的旁路堵死。
     */
    private void ensureDeletable(Ordered ordered) {
        if (ordered == null || !DELETABLE_STATUSES.contains(ordered.getStatus())) {
            throw new CustomException(ErrorCode.BUSINESS_CONFLICT,
                    "仅「已取消 / 已退票」订单可删除；已支付订单请走退票流程");
        }
    }

    /** 资金操作使用的订单金额。金额缺失属存量脏数据，给出可读提示而不是抛 NPE */
    private BigDecimal orderAmount(Ordered ordered) {
        if (ordered.getTotal() == null) {
            throw new CustomException(ErrorCode.SYSTEM_ERROR, "订单缺失金额，无法完成资金操作，请联系管理员");
        }
        return BigDecimal.valueOf(ordered.getTotal());
    }

    private void ensureOrderAccess(Ordered ordered, String role, Integer userId) {
        if (ordered == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("USER".equals(role) && userId != null && userId.equals(ordered.getUserId())) {
            return;
        }
        if ("CINEMA".equals(role) && userId != null && userId.equals(ordered.getCinemaId())) {
            return;
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "无权操作该订单");
    }

    /**
     * 规范化并校验座位号：合法范围取自所属影厅的 seat_rows/seat_cols，
     * 影厅已删除或未配置座位数时退回 8×8（与 room 表列默认值一致）。
     */
    private List<String> normalizeSeats(String rawSeats, Integer roomId) {
        if (rawSeats == null || rawSeats.trim().isEmpty()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择座位");
        }
        Set<String> seats = Arrays.stream(rawSeats.replace('，', ',').split(","))
                .map(String::trim)
                .filter(seat -> !seat.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (seats.isEmpty()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "请选择座位");
        }
        if (seats.size() > MAX_SEATS_PER_ORDER) {
            throw new CustomException(ErrorCode.PARAM_INVALID,
                    "单笔订单最多选择 " + MAX_SEATS_PER_ORDER + " 个座位");
        }

        Room room = roomId == null ? null : roomMapper.selectById(roomId);
        int rows = room != null && room.getSeatRows() != null ? room.getSeatRows() : DEFAULT_SEAT_ROWS;
        int cols = room != null && room.getSeatCols() != null ? room.getSeatCols() : DEFAULT_SEAT_COLS;

        for (String seat : seats) {
            Matcher matcher = SEAT_SHAPE.matcher(seat);
            if (!matcher.matches()) {
                throw new CustomException(ErrorCode.PARAM_INVALID, "座位格式错误: " + seat);
            }
            int row = Integer.parseInt(matcher.group(1));
            int col = Integer.parseInt(matcher.group(2));
            if (row < 1 || row > rows || col < 1 || col > cols) {
                throw new CustomException(ErrorCode.PARAM_INVALID,
                        "座位超出影厅范围: " + seat + "（本厅 " + rows + " 排 " + cols + " 座）");
            }
        }
        return List.copyOf(seats);
    }

    private BigDecimal parsePrice(String price) {
        try {
            return new BigDecimal(price).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SYSTEM_ERROR, "场次票价配置错误");
        }
    }

    private String generateOrderNo() {
        String date = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return date + suffix;
    }
}
