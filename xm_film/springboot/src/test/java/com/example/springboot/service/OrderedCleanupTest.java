package com.example.springboot.service;

import com.example.springboot.common.OrderStatusCriteria;
import com.example.springboot.entity.Ordered;
import com.example.springboot.mapper.CinemaMapper;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.RecordMapper;
import com.example.springboot.mapper.RoomMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 超时订单清理：定时任务最终落到的业务方法。
 *
 * <p>此前零覆盖。两个分支都要钉住：没有超时单时不能发批量 UPDATE；
 * 有超时单时只按查出来的 id 批量取消，不能顺手取消别的状态。
 */
@ExtendWith(MockitoExtension.class)
class OrderedCleanupTest {

    @Mock
    private OrderedMapper orderedMapper;
    @Mock
    private RecordMapper recordMapper;
    @Mock
    private FilmMapper filmMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private CinemaMapper cinemaMapper;
    @Mock
    private WalletService walletService;
    @Mock
    private PayPasswordService payPasswordService;

    @InjectMocks
    private OrderedService orderedService;

    @Test
    void noExpiredOrdersMeansNoBatchUpdate() {
        when(orderedMapper.selectExpiredPendingOrders(OrderStatusCriteria.INSTANCE)).thenReturn(List.of());

        orderedService.cancelExpiredPendingOrders();

        verify(orderedMapper, never()).batchCancelExpiredOrders(any(), any());
    }

    @Test
    void expiredOrdersAreCancelledById() {
        Ordered first = new Ordered();
        first.setId(11);
        Ordered second = new Ordered();
        second.setId(12);
        when(orderedMapper.selectExpiredPendingOrders(OrderStatusCriteria.INSTANCE))
                .thenReturn(List.of(first, second));

        orderedService.cancelExpiredPendingOrders();

        verify(orderedMapper).batchCancelExpiredOrders(List.of(11, 12), OrderStatusCriteria.INSTANCE);
    }
}
