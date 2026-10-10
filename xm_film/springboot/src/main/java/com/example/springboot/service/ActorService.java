package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Actor;
import com.example.springboot.mapper.ActorMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演员的通用增删改查，直接复用 BaseService 模板，无额外业务规则。
 */
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
