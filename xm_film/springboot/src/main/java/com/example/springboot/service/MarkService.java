package com.example.springboot.service;

import com.example.springboot.common.FileUtil;
import com.example.springboot.entity.Mark;
import com.example.springboot.mapper.MarkMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class MarkService {

    @Resource
    private MarkMapper markMapper;

    public List<Mark> selectAll(Mark mark) {
        return markMapper.selectAll(mark);
    }

    public Mark selectById(Integer id) {
        return markMapper.selectById(id);

    }

    public List<Mark> selectList(Mark mark) {
        System.out.println(mark);
        return null;
    }


    public PageInfo<Mark> selectPage( Mark mark, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Mark> list = markMapper.selectAll(mark);
        return PageInfo.of(list);
    }

    public void add(Mark mark) {
        markMapper.insert(mark);
    }

    public void update(Mark mark) {
        markMapper.updateById(mark);
    }

    public void delete(Integer id) {
        markMapper.deleteById(id);
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
