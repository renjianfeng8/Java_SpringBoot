package com.example.springboot;

import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 绑定层守卫：mvn compile 不解析 mapper XML，命名空间 / 语句 id / #{参数名} 写错都能编译通过，
 * 只有启动 Spring 上下文并真正执行语句时才会暴露。这里逐条跑一遍大盘用到的每条语句
 * —— 它们的单元测试一律 mock 掉 mapper，XML 本身在那边永远跑不到。
 * <p>
 * 全部是只读 COUNT / 聚合，不写库；窗口取一个不可能有数据的远古区间，
 * 保证对开发库零影响，可反复运行。
 */
@SpringBootTest
@ActiveProfiles("ci")
class StatisticsMapperLoadTest {

    @Autowired
    OrderedMapper orderedMapper;

    @Autowired
    UserMapper userMapper;

    @Test
    void mapperLoadsCountTodayPaidOrdersStatement() {
        assertThat(orderedMapper.countTodayPaidOrders()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void mapperLoadsSelectPaidRevenueByDayStatement() {
        List<Map<String, Object>> rows =
                orderedMapper.selectPaidRevenueByDay(LocalDate.of(1970, 1, 1), LocalDate.of(1970, 1, 2));
        assertThat(rows).isEmpty();
    }

    @Test
    void mapperLoadsCountByStatusStatement() {
        assertThat(orderedMapper.countByStatus("待取票")).isGreaterThanOrEqualTo(0);
    }

    @Test
    void mapperLoadsCountGroupByStatusStatement() {
        // 只断言「能跑通、返回的是 name/value 形状」：本机库里有几单、各是什么状态
        // 取决于有没有人下过单，钉死行数会让这条守卫随开发库的现状变红。
        assertThat(orderedMapper.countGroupByStatus())
                .allSatisfy(row -> assertThat(row).containsKeys("name", "value"));
    }

    @Test
    void mapperLoadsUserCountAllStatement() {
        assertThat(userMapper.countAll()).isGreaterThanOrEqualTo(0);
    }
}
