package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Ordered;
import com.example.springboot.service.OrderedService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单管理", description = "购票订单 CRUD")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderedController extends BaseController<Ordered> {
    public OrderedController(OrderedService orderedService) {
        super(orderedService);
    }
}
