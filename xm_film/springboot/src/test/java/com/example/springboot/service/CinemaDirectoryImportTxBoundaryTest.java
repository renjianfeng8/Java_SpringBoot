package com.example.springboot.service;

import com.example.springboot.common.AmapClient;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 守卫（架构修复 2）：名录导入的外部抓取必须发生在数据库事务<b>之外</b>。
 *
 * <p><b>缺陷</b>：原 {@code importAll} 整体标注 {@code @Transactional}，于是高德抓取
 * （最多 8 页 × N 个区县的外部 HTTP，实测一次约 3 秒）全程占着一个数据库连接和一个
 * 未提交事务。连接池被慢请求长期占用，并发导入互相排队。
 *
 * <p><b>判据</b>：在「抓取」被调用的那一刻读取
 * {@link TransactionSynchronizationManager#isActualTransactionActive()}。
 * 这是缺陷本身的可观测形式（抓取时有没有活跃事务），而不是实现细节：
 * 无论改成 TransactionTemplate、拆 bean 还是手工 begin/commit，只要抓取时事务已开就会变红。
 *
 * <p>不用 {@code information_schema.INNODB_TRX} 判定 —— 实测本机该表恒为 0（会给出假通过）。
 */
@SpringBootTest
@ActiveProfiles("ci")
class CinemaDirectoryImportTxBoundaryTest {

    private static final String POI = "TX-BOUNDARY-1";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void fetchingRunsWithoutActiveTransaction() throws Exception {
        AmapClient client = mock(AmapClient.class);
        CinemaDirectoryMapper mapper = mock(CinemaDirectoryMapper.class);
        when(mapper.selectExistingPoiIds(anyList())).thenReturn(List.of());
        when(mapper.insertIgnore(any())).thenReturn(1);

        AtomicInteger activeAtFetch = new AtomicInteger(-1);
        CountDownLatch inFetch = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        when(client.searchCinemas(anyString(), anyInt(), anyInt())).thenAnswer(inv -> {
            activeAtFetch.set(TransactionSynchronizationManager.isActualTransactionActive() ? 1 : 0);
            inFetch.countDown();
            release.await(20, TimeUnit.SECONDS);
            return List.of(new AmapClient.CinemaPoi(POI, "边界影城(测试)",
                    "北京市", "北京市", "通州区", "某路 1 号", "010-1"));
        });

        CinemaDirectoryImportService service =
                new CinemaDirectoryImportService(client, mapper, transactionTemplate);

        Thread worker = new Thread(() -> service.importAll(List.of("北京")), "import-worker");
        worker.start();

        assertThat(inFetch.await(20, TimeUnit.SECONDS)).as("抓取阶段应被调用").isTrue();
        release.countDown();
        worker.join(20_000);
        assertThat(worker.isAlive()).as("导入线程应在闸门打开后结束").isFalse();

        assertThat(activeAtFetch.get())
                .as("抓取外部 HTTP 时不得有活跃数据库事务（否则慢请求长期持有连接与锁）")
                .isZero();

        jdbcTemplate.update("DELETE FROM cinema_directory WHERE poi_id = ?", POI);
    }
}
