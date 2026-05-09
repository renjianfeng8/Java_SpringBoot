package com.example.springboot.service;

import com.example.springboot.common.FileUtil;
import com.example.springboot.entity.Film;
import com.example.springboot.entity.Record;
import com.example.springboot.mapper.RecordMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class RecordService {

    @Resource
    private RecordMapper recordMapper;

    @Resource
    private TypeService typeService;


    public List<Record> selectAll(Record record) {
        return recordMapper.selectAll(record);
    }


    public List<Record> selectList(Record record) {
        return recordMapper.selectAll(record);
    }


    public Record selectById(Integer id) {
        return recordMapper.selectById(id);
    }


    public PageInfo<Record> selectPage(Record record, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        List<Record> list = recordMapper.selectAll(record);

        return new PageInfo<>(list);
    }


    public void add(Record record) {
        recordMapper.insert(record);
    }


    public void update(Record record) {
        recordMapper.updateById(record);
    }

    public void delete(Integer id) {
        recordMapper.deleteById(id);
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