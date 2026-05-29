package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.entity.Video;
import com.example.springboot.service.VideoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController extends BaseController<Video> {
    public VideoController(VideoService videoService) {
        super(videoService);
    }
}
