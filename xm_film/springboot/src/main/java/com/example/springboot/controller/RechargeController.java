package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.dto.request.RechargeCallbackRequest;
import com.example.springboot.dto.request.RechargeCreateRequest;
import com.example.springboot.entity.RechargeOrder;
import com.example.springboot.service.RechargeService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public Result create(@Valid @RequestBody RechargeCreateRequest request) {
        return Result.success(rechargeService.createRecharge(
                AuthContext.userId(), AuthContext.role(), request.getAmount()));
    }

    @GetMapping("/page")
    public Result page(RechargeOrder entity,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageMethod.startPage(pageNum, pageSize);
        return Result.success(new PageInfo<>(
                rechargeService.selectScoped(entity, AuthContext.role(), AuthContext.userId())));
    }

    @PostMapping("/{id}/callback")
    public Result callback(@PathVariable Integer id, @Valid @RequestBody RechargeCallbackRequest request) {
        rechargeService.handleCallback(id, Boolean.TRUE.equals(request.getSuccess()),
                request.getRemark(), AuthContext.role(), AuthContext.userId());
        return Result.success();
    }
}
