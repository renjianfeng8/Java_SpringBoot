package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.dto.request.RechargeCallbackRequest;
import com.example.springboot.dto.request.RechargeCreateRequest;
import com.example.springboot.dto.request.RechargeOrderRequest;
import com.example.springboot.dto.response.RechargeOrderView;
import com.example.springboot.entity.RechargeOrder;
import com.example.springboot.service.RechargeService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 充值单据。不继承 BaseController：单据是资金凭证，不存在通用更新与删除，
 * 状态只能经回调端点流转。
 */
@RestController
@RequestMapping("/api/v1/recharges")
public class RechargeController {

    private final RechargeService rechargeService;

    public RechargeController(RechargeService rechargeService) {
        this.rechargeService = rechargeService;
    }

    @PostMapping
    public Result<RechargeOrderView> create(@Valid @RequestBody RechargeCreateRequest request) {
        return Result.success(toView(rechargeService.createRecharge(
                AuthContext.userId(), AuthContext.role(), request.getAmount())));
    }

    @GetMapping("/page")
    public Result<PageInfo<RechargeOrderView>> page(RechargeOrderRequest query,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageMethod.startPage(pageNum, pageSize);
        List<RechargeOrder> list = rechargeService.selectScoped(toEntity(query), AuthContext.role(), AuthContext.userId());
        PageInfo<RechargeOrder> src = new PageInfo<>(list);
        PageInfo<RechargeOrderView> dst = new PageInfo<>();
        BeanUtils.copyProperties(src, dst);
        dst.setList(src.getList().stream().map(this::toView).toList());
        return Result.success(dst);
    }

    @PostMapping("/{id}/callback")
    public Result<Void> callback(@PathVariable Integer id, @Valid @RequestBody RechargeCallbackRequest request) {
        rechargeService.handleCallback(id, Boolean.TRUE.equals(request.getSuccess()),
                request.getRemark(), AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    /** 请求 DTO → 实体：查询条件交给 Service/DAO */
    private RechargeOrder toEntity(RechargeOrderRequest request) {
        RechargeOrder entity = new RechargeOrder();
        entity.setId(request.getId());
        entity.setRechargeNo(request.getRechargeNo());
        entity.setUserId(request.getUserId());
        entity.setAmount(request.getAmount());
        entity.setStatus(request.getStatus());
        entity.setCreateTime(request.getCreateTime());
        entity.setFinishTime(request.getFinishTime());
        entity.setRemark(request.getRemark());
        return entity;
    }

    /** 实体 → 响应视图 */
    private RechargeOrderView toView(RechargeOrder entity) {
        RechargeOrderView view = new RechargeOrderView();
        view.setId(entity.getId());
        view.setRechargeNo(entity.getRechargeNo());
        view.setUserId(entity.getUserId());
        view.setUserName(entity.getUserName());
        view.setAmount(entity.getAmount());
        view.setStatus(entity.getStatus());
        view.setCreateTime(entity.getCreateTime());
        view.setFinishTime(entity.getFinishTime());
        view.setRemark(entity.getRemark());
        return view;
    }
}
