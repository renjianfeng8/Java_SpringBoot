package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Actor;
import com.example.springboot.mapper.ActorMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ActorService extends BaseService<Actor> {

    @Resource
    private ActorMapper actorMapper;

    @Override
    protected BaseMapper<Actor> mapper() {
        return actorMapper;
    }
}
