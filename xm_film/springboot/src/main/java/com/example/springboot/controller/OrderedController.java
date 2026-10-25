package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.PayResult;
import com.example.springboot.common.enums.RoleEnum;
import com.example.springboot.dto.request.OrderCreateRequest;
import com.example.springboot.dto.request.OrderPayRequest;
import com.example.springboot.dto.request.OrderedRequest;
import com.example.springboot.dto.response.OrderedView;
import com.example.springboot.dto.response.SeatOccupancyView;
import com.example.springboot.entity.Ordered;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.OrderedService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderedController extends BaseController<Ordered, OrderedRequest, OrderedView> {
    private final OrderedService orderedService;

    public OrderedController(OrderedService orderedService) {
        super(orderedService);
        this.orderedService = orderedService;
    }

    @Override
    protected Ordered toEntity(OrderedRequest request) {
        Ordered ordered = new Ordered();
        ordered.setId(request.getId());
        ordered.setOrders(request.getOrders());
        ordered.setRecordId(request.getRecordId());
        ordered.setUserId(request.getUserId());
        ordered.setFilmId(request.getFilmId());
        ordered.setImg(request.getImg());
        ordered.setCinemaId(request.getCinemaId());
        ordered.setRoomId(request.getRoomId());
        ordered.setAppointment(request.getAppointment());
        ordered.setTotal(request.getTotal());
        ordered.setUnitPrice(request.getUnitPrice());
        ordered.setNumber(request.getNumber());
        ordered.setStatus(request.getStatus());
        ordered.setStart(request.getStart());
        ordered.setSeat(request.getSeat());
        ordered.setCreateTime(request.getCreateTime());
        ordered.setPendingTimeoutAt(request.getPendingTimeoutAt());
        ordered.setPayTime(request.getPayTime());
        ordered.setPayAmount(request.getPayAmount());
        ordered.setRefundTime(request.getRefundTime());
        ordered.setRefundAmount(request.getRefundAmount());
        ordered.setPickupCode(request.getPickupCode());
        ordered.setUserName(request.getUserName());
        ordered.setFilmName(request.getFilmName());
        ordered.setCinemaName(request.getCinemaName());
        ordered.setRoomName(request.getRoomName());
        return ordered;
    }

    @Override
    protected OrderedView toView(Ordered ordered) {
        OrderedView view = new OrderedView();
        view.setId(ordered.getId());
        view.setOrders(ordered.getOrders());
        view.setRecordId(ordered.getRecordId());
        view.setUserId(ordered.getUserId());
        view.setFilmId(ordered.getFilmId());
        view.setImg(ordered.getImg());
        view.setCinemaId(ordered.getCinemaId());
        view.setRoomId(ordered.getRoomId());
        view.setAppointment(ordered.getAppointment());
        view.setTotal(ordered.getTotal());
        view.setUnitPrice(ordered.getUnitPrice());
        view.setNumber(ordered.getNumber());
        view.setStatus(ordered.getStatus());
        view.setStart(ordered.getStart());
        view.setSeat(ordered.getSeat());
        view.setCreateTime(ordered.getCreateTime());
        view.setPendingTimeoutAt(ordered.getPendingTimeoutAt());
        view.setPayTime(ordered.getPayTime());
        view.setPayAmount(ordered.getPayAmount());
        view.setRefundTime(ordered.getRefundTime());
        view.setRefundAmount(ordered.getRefundAmount());
        view.setPickupCode(ordered.getPickupCode());
        view.setUserName(ordered.getUserName());
        view.setFilmName(ordered.getFilmName());
        view.setCinemaName(ordered.getCinemaName());
        view.setRoomName(ordered.getRoomName());
        return view;
    }

    @Override
    @GetMapping
    public Result<List<OrderedView>> list(OrderedRequest query) {
        limitList();
        Ordered entity = toEntity(query);
        orderedService.applyScope(entity, AuthContext.role(), AuthContext.userId());
        return Result.success(orderedService.selectAll(entity).stream().map(this::toView).toList());
    }

    @Override
    @GetMapping("/page")
    public Result<PageInfo<OrderedView>> page(OrderedRequest query,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        Ordered entity = toEntity(query);
        orderedService.applyScope(entity, AuthContext.role(), AuthContext.userId());
        PageMethod.startPage(pageNum, pageSize);
        return Result.success(toPageView(new PageInfo<>(orderedService.selectAll(entity))));
    }

    @Override
    @GetMapping("/{id}")
    public Result<OrderedView> getById(@PathVariable Integer id) {
        Ordered ordered = orderedService.selectByIdScoped(id, AuthContext.role(), AuthContext.userId());
        return Result.success(toView(ordered));
    }

    @Override
    @PostMapping
    public Result<Void> add(@RequestBody OrderedRequest request) {
        throw new CustomException(ErrorCode.PARAM_INVALID, "请使用 /api/v1/orders/create 创建订单");
    }

    @PostMapping("/create")
    public Result<OrderedView> create(@Valid @RequestBody OrderCreateRequest request) {
        Ordered ordered = new Ordered();
        ordered.setRecordId(request.getRecordId());
        ordered.setSeat(request.getSeat());
        orderedService.createOrder(ordered, AuthContext.role(), AuthContext.userId());
        return Result.success(toView(ordered));
    }

    @Override
    @PutMapping
    public Result<Void> update(@RequestBody OrderedRequest request) {
        orderedService.updateScoped(toEntity(request), AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Integer id) {
        orderedService.cancelOrder(id, AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @PutMapping("/{id}/pickup")
    public Result<Void> pickup(@PathVariable Integer id) {
        orderedService.pickupOrder(id, AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @PutMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Integer id, @Valid @RequestBody OrderPayRequest request) {
        if (orderedService.payOrder(id, AuthContext.role(), AuthContext.userId(), request.getPayPassword())
                == PayResult.TIMEOUT_CANCELLED) {
            // 超时取消必须已落库，因此服务层不抛异常回滚，由此处翻译成业务错误返回客户端
            return Result.error(ErrorCode.BUSINESS_CONFLICT, "支付超时，订单已自动取消");
        }
        return Result.success();
    }

    @PutMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Integer id) {
        orderedService.refundOrder(id, AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @GetMapping("/seats")
    public Result<List<SeatOccupancyView>> seats(@RequestParam Integer recordId) {
        // 归属判定在后端按 JWT 完成；响应里不含他人订单的订单号/用户/金额。
        // mine 只对 USER 有意义：cinema/admin 的 id 与 user 表不同源，直接把 id 传下去
        // 会把同号用户订单误标成"我的"，泄露其订单号/金额 —— 非 USER 一律按匿名视角查。
        Integer viewerId = RoleEnum.USER.name().equals(AuthContext.role()) ? AuthContext.userId() : null;
        return Result.success(orderedService.selectSeatOccupancy(recordId, viewerId));
    }

    @Override
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        orderedService.deleteScoped(id, AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        orderedService.deleteBatchScoped(ids, AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

}
