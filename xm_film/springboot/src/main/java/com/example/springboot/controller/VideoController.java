package com.example.springboot.controller;

import com.example.springboot.common.BaseController;
import com.example.springboot.dto.request.VideoRequest;
import com.example.springboot.dto.response.VideoView;
import com.example.springboot.entity.Video;
import com.example.springboot.service.VideoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController extends BaseController<Video, VideoRequest, VideoView> {
    public VideoController(VideoService videoService) {
        super(videoService);
    }

    @Override
    protected Video toEntity(VideoRequest request) {
        Video video = new Video();
        video.setId(request.getId());
        video.setTitle(request.getTitle());
        video.setImg(request.getImg());
        video.setName(request.getName());
        video.setPreview(request.getPreview());
        video.setStart(request.getStart());
        return video;
    }

    @Override
    protected VideoView toView(Video video) {
        VideoView view = new VideoView();
        view.setId(video.getId());
        view.setTitle(video.getTitle());
        view.setImg(video.getImg());
        view.setName(video.getName());
        view.setPreview(video.getPreview());
        view.setStart(video.getStart());
        return view;
    }
}
