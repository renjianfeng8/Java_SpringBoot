package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Ordered;
import com.example.springboot.service.OrderedService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderedController extends BaseController<Ordered> {
    public OrderedController(OrderedService orderedService) {
        super(orderedService);
    }
}
