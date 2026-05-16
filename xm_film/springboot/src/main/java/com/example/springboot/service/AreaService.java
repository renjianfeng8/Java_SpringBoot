package com.example.springboot.service;

import com.example.springboot.entity.Area;
import com.example.springboot.mapper.AreaMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AreaService {

    @Resource
    private AreaMapper areaMapper;

    public List<Area> selectAll(Area area) {
        return areaMapper.selectAll(area);
    }

    public Area selectById(Integer id) {
        return areaMapper.selectById(id);

    }

    public List<Area> selectList(Area area) {
        return areaMapper.selectAll(area);
    }

    public PageInfo<Area> selectPage( Area area, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Area> list = areaMapper.selectAll(area);
        return PageInfo.of(list);
    }

    public void add(Area area) {
        areaMapper.insert(area);
    }

    public void update(Area area) {
        areaMapper.updateById(area);
    }

    public void delete(Integer id) {
        areaMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.delete(id);
        }
    }

}
