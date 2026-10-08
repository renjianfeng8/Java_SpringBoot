package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Actor;
import com.example.springboot.service.ActorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/actors")
public class ActorController extends BaseController<Actor> {
    public ActorController(ActorService actorService) {
        super(actorService);
    }
}
