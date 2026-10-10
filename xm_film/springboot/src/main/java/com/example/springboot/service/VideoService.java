package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Video;
import com.example.springboot.mapper.VideoMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 影片预告片的通用增删改查，直接复用 BaseService 模板，无额外业务规则。
 */
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
