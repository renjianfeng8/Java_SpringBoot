package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.FundFlow;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FundFlowMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/** 资金流水查询。账本只读，且只对本人（或管理员）可见。 */
@Service
public class FundFlowService {

    @Resource
    private FundFlowMapper fundFlowMapper;

    public List<FundFlow> selectScoped(FundFlow query, String role, Integer tokenUserId) {
        FundFlow condition = query == null ? new FundFlow() : query;
        if ("ADMIN".equals(role)) {
            return fundFlowMapper.selectAll(condition);
        }
        if ("USER".equals(role)) {
            // 一律以 JWT 里的 userId 为准，不接受前端传参，否则可以越权读别人的账
            condition.setUserId(tokenUserId);
            return fundFlowMapper.selectAll(condition);
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "无权查看资金流水");
    }
}
