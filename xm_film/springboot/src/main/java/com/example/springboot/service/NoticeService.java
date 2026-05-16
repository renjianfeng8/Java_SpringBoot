package com.example.springboot.service;

import com.example.springboot.entity.Notice;
import com.example.springboot.mapper.NoticeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    public List<Notice> selectAll(Notice notice) {
        return noticeMapper.selectAll(notice);
    }

    public Notice selectById(Integer id) {
        return noticeMapper.selectById(id);

    }

    public List<Notice> selectList(Notice notice) {
        return noticeMapper.selectAll(notice);
    }

    public PageInfo<Notice> selectPage( Notice notice, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Notice> list = noticeMapper.selectAll(notice);
        return PageInfo.of(list);
    }

    public void add(Notice notice) {
        noticeMapper.insert(notice);
    }

    public void update(Notice notice) {
        noticeMapper.updateById(notice);
    }

    public void delete(Integer id) {
        noticeMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.delete(id);
        }
    }

}
