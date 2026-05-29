package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Video;
import com.example.springboot.service.VideoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "预告片管理", description = "视频/预告片 CRUD")
@RestController
@RequestMapping("/api/v1/videos")
public class VideoController extends BaseController<Video> {
    public VideoController(VideoService videoService) {
        super(videoService);
    }
}
