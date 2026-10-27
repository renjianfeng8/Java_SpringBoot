package com.example.springboot.task;

import com.example.springboot.service.OrderedService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 定时任务此前零覆盖。任务本身只有一行委派，测试的价值是钉住
 * 「调度方法确实会调用清理逻辑」—— 改方法名/换 service 时编译期不会报错。
 */
class OrderCleanupTaskTest {

    @Test
    void cleanupDelegatesToOrderedService() {
        OrderedService orderedService = mock(OrderedService.class);
        OrderCleanupTask task = new OrderCleanupTask(orderedService);

        task.cleanupExpiredPendingPayments();

        verify(orderedService).cancelExpiredPendingOrders();
    }
}
