package com.example.springboot.service;

import com.example.springboot.entity.Type;
import com.example.springboot.mapper.TypeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 类型服务的唯一额外规则：空 id 列表不能下发 {@code IN ()}。
 */
@ExtendWith(MockitoExtension.class)
class TypeServiceTest {

    @Mock
    private TypeMapper typeMapper;

    @InjectMocks
    private TypeService typeService;

    @Test
    void emptyIdsReturnEmptyWithoutHittingMapper() {
        assertThat(typeService.selectByIds(null)).isEmpty();
        assertThat(typeService.selectByIds(List.of())).isEmpty();

        verify(typeMapper, never()).selectByIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void nonEmptyIdsDelegateToMapper() {
        Type type = new Type();
        type.setId(1);
        type.setTitle("科幻");
        when(typeMapper.selectByIds(List.of(1))).thenReturn(List.of(type));

        assertThat(typeService.selectByIds(List.of(1))).containsExactly(type);
    }
}
