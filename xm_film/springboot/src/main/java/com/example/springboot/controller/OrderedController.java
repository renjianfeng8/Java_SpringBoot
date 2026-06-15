package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.dto.request.OrderCreateRequest;
import com.example.springboot.entity.Ordered;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.OrderedService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订单管理", description = "购票订单 CRUD")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderedController extends BaseController<Ordered> {
    private final OrderedService orderedService;

    public OrderedController(OrderedService orderedService) {
        super(orderedService);
        this.orderedService = orderedService;
    }

    @Override
    @GetMapping
    public Result list(Ordered entity) {
        orderedService.applyScope(entity, currentRole(), currentUserId());
        return Result.success(orderedService.selectAll(entity));
    }

    @Override
    @GetMapping("/page")
    public Result page(Ordered entity,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        orderedService.applyScope(entity, currentRole(), currentUserId());
        PageHelper.startPage(pageNum, pageSize);
        return Result.success(new PageInfo<>(orderedService.selectAll(entity)));
    }

    @Override
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        return Result.success(orderedService.selectByIdScoped(id, currentRole(), currentUserId()));
    }

    @Override
    @PostMapping
    public Result add(@RequestBody Ordered entity) {
        throw new CustomException(ErrorCode.PARAM_INVALID, "请使用 /api/v1/orders/create 创建订单");
    }

    @PostMapping("/create")
    public Result create(@Valid @RequestBody OrderCreateRequest request) {
        Ordered ordered = new Ordered();
        ordered.setRecordId(request.getRecordId());
        ordered.setSeat(request.getSeat());
        orderedService.createOrder(ordered, currentRole(), currentUserId());
        return Result.success();
    }

    @Override
    @PutMapping
    public Result update(@RequestBody Ordered entity) {
        orderedService.updateScoped(entity, currentRole(), currentUserId());
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result cancel(@PathVariable Integer id) {
        orderedService.cancelOrder(id, currentRole(), currentUserId());
        return Result.success();
    }

    @PutMapping("/{id}/pickup")
    public Result pickup(@PathVariable Integer id) {
        orderedService.pickupOrder(id, currentRole(), currentUserId());
        return Result.success();
    }

    @GetMapping("/seats")
    public Result seats(@RequestParam Integer recordId) {
        List<Ordered> orders = orderedService.selectActiveByRecordId(recordId);
        return Result.success(orders);
    }

    @Override
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        orderedService.deleteScoped(id, currentRole(), currentUserId());
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        orderedService.deleteBatchScoped(ids, currentRole(), currentUserId());
        return Result.success();
    }

    private String currentRole() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : (String) request.getAttribute("role");
    }

    private Integer currentUserId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String userId = (String) request.getAttribute("userId");
        return userId == null ? null : Integer.valueOf(userId);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
