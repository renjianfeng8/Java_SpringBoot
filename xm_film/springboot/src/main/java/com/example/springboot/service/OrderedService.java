package com.example.springboot.service;

import com.example.springboot.common.FileUtil;
import com.example.springboot.entity.Ordered;
import com.example.springboot.mapper.OrderedMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class OrderedService {

    @Resource
    private OrderedMapper orderedMapper;

    @Resource
    private TypeService typeService;


    public List<Ordered> selectAll(Ordered ordered) {
        return orderedMapper.selectAll(ordered);
    }


    public List<Ordered> selectList(Ordered ordered) {
        return orderedMapper.selectAll(ordered);
    }


    public Ordered selectById(Integer id) {
        return orderedMapper.selectById(id);
    }


    public PageInfo<Ordered> selectPage(Ordered ordered, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        List<Ordered> list = orderedMapper.selectAll(ordered);

        return new PageInfo<>(list);
    }


    public void add(Ordered ordered) {
        orderedMapper.insert(ordered);
    }


    public void update(Ordered ordered) {
        orderedMapper.updateById(ordered);
    }

    public void delete(Integer id) {
        orderedMapper.deleteById(id);
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