package com.example.springboot.service;

import com.example.springboot.common.FileUtil;
import com.example.springboot.entity.Actor;
import com.example.springboot.mapper.ActorMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ActorService {

    @Resource
    private ActorMapper actorMapper;

    public List<Actor> selectAll(Actor actor) {
        return actorMapper.selectAll(actor);
    }

    public Actor selectById(Integer id) {
        return actorMapper.selectById(id);

    }

    public List<Actor> selectList(Actor actor) {
        System.out.println(actor);
        return null;
    }

    public PageInfo<Actor> selectPage( Actor actor, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Actor> list = actorMapper.selectAll(actor);
        return PageInfo.of(list);
    }

    public void add(Actor actor) {
        actorMapper.insert(actor);
    }

    public void update(Actor actor) {
        actorMapper.updateById(actor);
    }

    public void delete(Integer id) {
        actorMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.delete(id);
        }
    }

    public String uploadFile(MultipartFile file, String uploadDir) throws IOException {
        return FileUtil.uploadFile(file, uploadDir);
    }

}
