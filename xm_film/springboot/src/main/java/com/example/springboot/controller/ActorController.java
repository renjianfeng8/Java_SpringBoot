package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Actor;
import com.example.springboot.service.ActorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "演员管理", description = "演职人员 CRUD")
@RestController
@RequestMapping("/api/v1/actors")
public class ActorController extends BaseController<Actor> {
    public ActorController(ActorService actorService) {
        super(actorService);
    }
}
