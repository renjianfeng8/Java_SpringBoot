package com.example.springboot.service;

import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 影院名录只读服务：分页与城市/品牌去重，全部是 mapper 的薄委派。
 */
@ExtendWith(MockitoExtension.class)
class CinemaDirectoryServiceTest {

    @Mock
    private CinemaDirectoryMapper mapper;

    @InjectMocks
    private CinemaDirectoryService service;

    @AfterEach
    void clearPage() {
        // PageHelper 的 ThreadLocal 只在 SQL 执行后清理；这里用的 mock 不会触发拦截器
        PageMethod.clearPage();
    }

    @Test
    void selectPageDelegatesAndReturnsPageInfo() {
        CinemaDirectory directory = new CinemaDirectory();
        directory.setId(1);
        directory.setName("万达影城");
        when(mapper.selectAll(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(directory));

        PageInfo<CinemaDirectory> page = service.selectPage(new CinemaDirectory(), 1, 12);

        assertThat(page.getList()).containsExactly(directory);
        assertThat(page.getTotal()).isEqualTo(1);
    }

    @Test
    void distinctValuesDelegate() {
        when(mapper.distinctCities()).thenReturn(List.of("北京"));
        when(mapper.distinctBrands()).thenReturn(List.of("万达"));

        assertThat(service.distinctCities()).containsExactly("北京");
        assertThat(service.distinctBrands()).containsExactly("万达");

        verify(mapper).distinctCities();
        verify(mapper).distinctBrands();
    }
}
