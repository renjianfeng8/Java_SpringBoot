package com.example.springboot.service;

import com.example.springboot.common.FileUtil;
import com.example.springboot.entity.Video;
import com.example.springboot.mapper.VideoMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class VideoService {

    @Resource
    private VideoMapper videoMapper;

    public List<Video> selectAll(Video video) {
        return videoMapper.selectAll(video);
    }

    public Video selectById(Integer id) {
        return videoMapper.selectById(id);

    }

    public List<Video> selectList(Video video) {
        return videoMapper.selectAll(video);
    }

    public PageInfo<Video> selectPage( Video video, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Video> list = videoMapper.selectAll(video);
        return PageInfo.of(list);
    }

    public void add(Video video) {
        videoMapper.insert(video);
    }

    public void update(Video video) {
        videoMapper.updateById(video);
    }

    public void delete(Integer id) {
        videoMapper.deleteById(id);
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
