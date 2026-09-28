package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.entity.FundFlow;
import com.example.springboot.service.FundFlowService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资金流水。只读账本：不提供任何写入、更新、删除端点。
 */
@Tag(name = "资金流水", description = "账户余额变动明细（充值/购票/退票），只读")
@RestController
@RequestMapping("/api/v1/fund-flows")
public class FundFlowController {

    private final FundFlowService fundFlowService;

    public FundFlowController(FundFlowService fundFlowService) {
        this.fundFlowService = fundFlowService;
    }

    @Operation(summary = "分页查询资金流水", description = "USER 只看自己的，ADMIN 看全部")
    @GetMapping("/page")
    public Result page(FundFlow entity,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageMethod.startPage(pageNum, pageSize);
        return Result.success(new PageInfo<>(
                fundFlowService.selectScoped(entity, AuthContext.role(), AuthContext.userId())));
    }
}
