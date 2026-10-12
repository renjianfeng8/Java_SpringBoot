package com.example.springboot.service;

import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 影院名录（高德导入的原始资料）的只读查询：分页与去重后的城市/品牌取值。
 * 名录的抓取与落库在 {@link CinemaDirectoryImportService}，本类不写库。
 */
@Service
@Transactional(readOnly = true)
public class CinemaDirectoryService {

    private final CinemaDirectoryMapper mapper;

    public CinemaDirectoryService(CinemaDirectoryMapper mapper) {
        this.mapper = mapper;
    }

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
