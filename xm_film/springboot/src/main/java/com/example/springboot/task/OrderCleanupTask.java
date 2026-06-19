package com.example.springboot.task;

import com.example.springboot.service.OrderedService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(OrderCleanupTask.class);

    @Resource
    private OrderedService orderedService;

    @Scheduled(fixedDelay = 15_000)
    public void cleanupExpiredPendingPayments() {
        log.debug("清理超时未支付订单...");
        orderedService.cancelExpiredPendingOrders();
    }
}
