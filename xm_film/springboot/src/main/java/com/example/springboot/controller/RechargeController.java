package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.dto.request.RechargeCallbackRequest;
import com.example.springboot.dto.request.RechargeCreateRequest;
import com.example.springboot.entity.RechargeOrder;
import com.example.springboot.service.RechargeService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 充值单据。不继承 BaseController：单据是资金凭证，不存在通用更新与删除，
 * 状态只能经回调端点流转。
 */
@Tag(name = "充值单据", description = "充值申请 / 单据查询 / 模拟支付回调")
@RestController
@RequestMapping("/api/v1/recharges")
public class RechargeController {

    private final RechargeService rechargeService;

    public RechargeController(RechargeService rechargeService) {
        this.rechargeService = rechargeService;
    }

    @Operation(summary = "提交充值申请", description = "只生成「处理中」单据，余额不变；充值人取自 JWT")
    @PostMapping
    public Result create(@Valid @RequestBody RechargeCreateRequest request) {
        return Result.success(rechargeService.createRecharge(
                AuthContext.userId(), AuthContext.role(), request.getAmount()));
    }

    @Operation(summary = "分页查询充值单据", description = "USER 只看自己的，ADMIN 看全部")
    @GetMapping("/page")
    public Result page(RechargeOrder entity,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageMethod.startPage(pageNum, pageSize);
        return Result.success(new PageInfo<>(
                rechargeService.selectScoped(entity, AuthContext.role(), AuthContext.userId())));
    }

    @Operation(summary = "模拟支付网关回调",
            description = "仅「处理中」单据可处理，重复回调被拒；成功则入账并写资金流水")
    @PostMapping("/{id}/callback")
    public Result callback(@PathVariable Integer id, @Valid @RequestBody RechargeCallbackRequest request) {
        rechargeService.handleCallback(id, Boolean.TRUE.equals(request.getSuccess()),
                request.getRemark(), AuthContext.role(), AuthContext.userId());
        return Result.success();
    }
}
