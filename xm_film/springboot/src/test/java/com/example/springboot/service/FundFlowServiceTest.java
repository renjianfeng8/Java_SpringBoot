package com.example.springboot.service;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.entity.FundFlow;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FundFlowMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资金流水的可见范围：普通用户只能看自己的，且不受请求参数影响
 * （前端传 userId 不能越权读别人的账）。
 */
@ExtendWith(MockitoExtension.class)
class FundFlowServiceTest {

    private static final Integer USER_ID = 6;

    @Mock
    private FundFlowMapper fundFlowMapper;

    @InjectMocks
    private FundFlowService fundFlowService;

    @Test
    void selectScopedAsUserShouldForceOwnUserIdIgnoringRequestParam() {
        FundFlow query = new FundFlow();
        query.setUserId(999);
        when(fundFlowMapper.selectAll(any())).thenReturn(List.of());

        fundFlowService.selectScoped(query, "USER", USER_ID);

        assertEquals(USER_ID, captureCondition().getUserId());
    }

    @Test
    void selectScopedAsUserWithNullQueryShouldStillScopeToSelf() {
        when(fundFlowMapper.selectAll(any())).thenReturn(List.of());

        fundFlowService.selectScoped(null, "USER", USER_ID);

        assertEquals(USER_ID, captureCondition().getUserId());
    }

    @Test
    void selectScopedAsAdminShouldNotFilterByUser() {
        when(fundFlowMapper.selectAll(any())).thenReturn(List.of());

        fundFlowService.selectScoped(null, "ADMIN", 1);

        assertNull(captureCondition().getUserId());
    }

    @Test
    void selectScopedAsCinemaShouldThrowForbidden() {
        CustomException ex = assertThrows(CustomException.class,
                () -> fundFlowService.selectScoped(null, "CINEMA", 5));

        assertEquals(ErrorCode.FORBIDDEN.code(), ex.getCode());
        verify(fundFlowMapper, never()).selectAll(any());
    }

    private FundFlow captureCondition() {
        ArgumentCaptor<FundFlow> captor = ArgumentCaptor.forClass(FundFlow.class);
        verify(fundFlowMapper).selectAll(captor.capture());
        return captor.getValue();
    }
}
