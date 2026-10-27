package com.example.springboot;

import com.example.springboot.entity.Type;
import com.example.springboot.mapper.TypeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 分类名唯一：库层 uk_type_title 兜底。没有它，管理端可以把「剧情」重复建两次，
 * 前台分类下拉与筛选都会出现重复项。
 */
@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class TypeTitleUniqueTest {

    private static final String PROBE_TITLE = "__type_unique_probe__";

    @Autowired
    TypeMapper typeMapper;

    @Test
    void duplicateTitleRejectedByDatabase() {
        typeMapper.insert(new Type(null, PROBE_TITLE));

        assertThatThrownBy(() -> typeMapper.insert(new Type(null, PROBE_TITLE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
