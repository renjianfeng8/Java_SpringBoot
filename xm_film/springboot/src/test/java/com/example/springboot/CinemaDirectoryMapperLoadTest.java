package com.example.springboot;

import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 绑定层守卫：mvn compile 不解析 mapper XML，selectAll 里「EXISTS(...) AS linked」这种派生列
 * 与 d. 前缀写错都要启动 Spring 上下文、真正执行语句才会暴露；CinemaServiceTest 那边把 mapper
 * mock 掉了，XML 本身永远跑不到。
 * <p>
 * 全部是只读查询，不写库。断言一律取「与库里现有行数无关」的不变量
 * ——钉死具体条数会让这条守卫随开发库现状变红；ci 库是空表，故不能断言「有行」。
 */
@SpringBootTest
@ActiveProfiles("ci")
class CinemaDirectoryMapperLoadTest {

    @Autowired
    CinemaDirectoryMapper mapper;

    @Test
    void selectAllDerivesLinkedFlagForEveryRow() {
        List<CinemaDirectory> rows = mapper.selectAll(new CinemaDirectory());
        assertThat(rows).extracting(CinemaDirectory::getLinked).doesNotContainNull();
    }

    @Test
    void linkedFilterReturnsOnlyLinkedRows() {
        CinemaDirectory query = new CinemaDirectory();
        query.setLinked(true);
        assertThat(mapper.selectAll(query)).allSatisfy(row -> assertThat(row.getLinked()).isTrue());
    }

    @Test
    void unlinkedFilterReturnsOnlyUnlinkedRows() {
        CinemaDirectory query = new CinemaDirectory();
        query.setLinked(false);
        assertThat(mapper.selectAll(query)).allSatisfy(row -> assertThat(row.getLinked()).isFalse());
    }

    /** 两个分区互斥且穷尽：行数之和等于不筛选时的总数 */
    @Test
    void linkedPartitionsSumToTotal() {
        int total = mapper.selectAll(new CinemaDirectory()).size();

        CinemaDirectory linked = new CinemaDirectory();
        linked.setLinked(true);
        CinemaDirectory unlinked = new CinemaDirectory();
        unlinked.setLinked(false);

        assertThat(mapper.selectAll(linked).size() + mapper.selectAll(unlinked).size()).isEqualTo(total);
    }
}
