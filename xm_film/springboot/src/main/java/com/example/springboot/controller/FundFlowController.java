package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.Result;
import com.example.springboot.dto.request.FundFlowRequest;
import com.example.springboot.dto.response.FundFlowView;
import com.example.springboot.entity.FundFlow;
import com.example.springboot.service.FundFlowService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 资金流水。只读账本：不提供任何写入、更新、删除端点。
 */
@RestController
@RequestMapping("/api/v1/fund-flows")
public class FundFlowController {

    private final FundFlowService fundFlowService;

    public FundFlowController(FundFlowService fundFlowService) {
        this.fundFlowService = fundFlowService;
    }

    @GetMapping("/page")
    public Result<PageInfo<FundFlowView>> page(FundFlowRequest query,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageMethod.startPage(pageNum, pageSize);
        List<FundFlow> list = fundFlowService.selectScoped(toEntity(query), AuthContext.role(), AuthContext.userId());
        PageInfo<FundFlow> src = new PageInfo<>(list);
        PageInfo<FundFlowView> dst = new PageInfo<>();
        BeanUtils.copyProperties(src, dst);
        dst.setList(src.getList().stream().map(this::toView).toList());
        return Result.success(dst);
    }

    /** 请求 DTO → 实体：查询条件交给 Service/DAO */
    private FundFlow toEntity(FundFlowRequest request) {
        FundFlow entity = new FundFlow();
        entity.setId(request.getId());
        entity.setUserId(request.getUserId());
        entity.setSource(request.getSource());
        entity.setChangeAmount(request.getChangeAmount());
        entity.setBalanceBefore(request.getBalanceBefore());
        entity.setBalanceAfter(request.getBalanceAfter());
        entity.setRelatedId(request.getRelatedId());
        entity.setCreateTime(request.getCreateTime());
        return entity;
    }

    /** 实体 → 响应视图 */
    private FundFlowView toView(FundFlow entity) {
        FundFlowView view = new FundFlowView();
        view.setId(entity.getId());
        view.setUserId(entity.getUserId());
        view.setSource(entity.getSource());
        view.setChangeAmount(entity.getChangeAmount());
        view.setBalanceBefore(entity.getBalanceBefore());
        view.setBalanceAfter(entity.getBalanceAfter());
        view.setRelatedId(entity.getRelatedId());
        view.setCreateTime(entity.getCreateTime());
        return view;
    }
}
