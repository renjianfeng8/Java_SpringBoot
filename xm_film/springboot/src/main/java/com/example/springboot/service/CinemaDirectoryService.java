package com.example.springboot.service;

import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CinemaDirectoryService {

    @Resource
    private CinemaDirectoryMapper mapper;

    public PageInfo<CinemaDirectory> selectPage(CinemaDirectory query, Integer pageNum, Integer pageSize) {
        PageMethod.startPage(pageNum, pageSize);
        return PageInfo.of(mapper.selectAll(query));
    }

    public List<String> distinctCities() {
        return mapper.distinctCities();
    }

    public List<String> distinctBrands() {
        return mapper.distinctBrands();
    }
}
