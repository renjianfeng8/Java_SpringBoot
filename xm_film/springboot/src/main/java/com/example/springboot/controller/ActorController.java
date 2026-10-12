package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.dto.request.ActorRequest;
import com.example.springboot.dto.response.ActorView;
import com.example.springboot.entity.Actor;
import com.example.springboot.service.ActorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/actors")
public class ActorController extends BaseController<Actor, ActorRequest, ActorView> {
    public ActorController(ActorService actorService) {
        super(actorService);
    }

    @Override
    protected Actor toEntity(ActorRequest request) {
        Actor actor = new Actor();
        actor.setId(request.getId());
        actor.setTitle(request.getTitle());
        actor.setImg(request.getImg());
        actor.setActorName(request.getActorName());
        actor.setFigure(request.getFigure());
        actor.setPicture(request.getPicture());
        actor.setGrade(request.getGrade());
        actor.setVideo(request.getVideo());
        return actor;
    }

    @Override
    protected ActorView toView(Actor actor) {
        ActorView view = new ActorView();
        view.setId(actor.getId());
        view.setTitle(actor.getTitle());
        view.setImg(actor.getImg());
        view.setActorName(actor.getActorName());
        view.setFigure(actor.getFigure());
        view.setPicture(actor.getPicture());
        view.setGrade(actor.getGrade());
        view.setVideo(actor.getVideo());
        return view;
    }
}
