package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Video;
import com.example.springboot.mapper.VideoMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VideoService extends BaseService<Video> {

    @Resource
    private VideoMapper videoMapper;

    @Override
    protected BaseMapper<Video> mapper() {
        return videoMapper;
    }
}
