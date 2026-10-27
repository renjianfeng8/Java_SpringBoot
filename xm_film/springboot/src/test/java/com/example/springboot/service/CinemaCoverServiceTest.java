package com.example.springboot.service;

import com.example.springboot.common.AmapClient;
import com.example.springboot.entity.Cinema;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import com.example.springboot.mapper.CinemaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 影院封面补齐此前只被 mock 使用、没有直接测试。
 *
 * <p>这条通路的约定是「失败不抛」：高德下架、图片 404、落盘失败都只让这家影院
 * 继续用默认图，绝不能把整批导入回滚。测试就钉住这个「返回 false 而不是抛」。
 */
@ExtendWith(MockitoExtension.class)
class CinemaCoverServiceTest {

    @Mock
    private AmapClient amapClient;
    @Mock
    private CinemaMapper cinemaMapper;
    @Mock
    private CinemaDirectoryMapper directoryMapper;
    @Mock
    private TransactionTemplate transactionTemplate;

    @TempDir
    Path uploadDir;

    private CinemaCoverService service() {
        return new CinemaCoverService(amapClient, cinemaMapper, directoryMapper,
                transactionTemplate, uploadDir.toString(), "/files/");
    }

    @Test
    void lookupCoverUrlSwallowsAmapFailure() {
        when(amapClient.firstPhotoUrl("poi-1")).thenThrow(new IllegalStateException("amap down"));

        assertThat(service().lookupCoverUrl("poi-1")).isNull();
    }

    @Test
    void fillOneCoverWithoutDirectoryReturnsFalse() {
        Cinema cinema = new Cinema();
        cinema.setId(7);
        when(cinemaMapper.selectById(7)).thenReturn(cinema);

        assertThat(service().fillOneCover(7, null)).isFalse();
        verify(amapClient, never()).firstPhotoUrl(anyString());
    }

    @Test
    void fillOneCoverWithoutUrlReturnsFalse() {
        when(amapClient.firstPhotoUrl("poi-1")).thenReturn(Optional.empty());

        assertThat(service().fillOneCover(7, "poi-1")).isFalse();
        verify(amapClient, never()).downloadImage(anyString());
    }

    @Test
    void fillOneCoverWithoutBytesReturnsFalse() {
        when(amapClient.firstPhotoUrl("poi-1")).thenReturn(Optional.of("http://img/1.jpg"));
        when(amapClient.downloadImage("http://img/1.jpg")).thenReturn(Optional.empty());

        assertThat(service().fillOneCover(7, "poi-1")).isFalse();
        verify(cinemaMapper, never()).updateAvatar(any(), anyString());
    }

    /** 成功路径：简历名录 poiId → 下载 → 落盘 upload-dir → 只把写库包进事务 */
    @Test
    void fillOneCoverStoresFileAndUpdatesAvatar() {
        Cinema cinema = new Cinema();
        cinema.setId(7);
        cinema.setDirectoryId(3);
        CinemaDirectory directory = new CinemaDirectory();
        directory.setId(3);
        directory.setPoiId("poi-9");
        when(cinemaMapper.selectById(7)).thenReturn(cinema);
        when(directoryMapper.selectByIds(List.of(3))).thenReturn(List.of(directory));
        when(amapClient.firstPhotoUrl("poi-9")).thenReturn(Optional.of("http://img/9.jpg"));
        when(amapClient.downloadImage("http://img/9.jpg")).thenReturn(Optional.of(new byte[]{1, 2, 3}));
        doAnswer(invocation -> {
            invocation.getArgument(0, java.util.function.Consumer.class).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        assertThat(service().fillOneCover(7, null)).isTrue();

        verify(cinemaMapper).updateAvatar(eq(7), org.mockito.ArgumentMatchers.startsWith("/files/"));
        assertThat(uploadDir.toFile().listFiles()).hasSize(1);
    }

    /** 单家失败不中断整批：只有成功的那家计入 updated */
    @Test
    void backfillCountsOnlySuccessfulCovers() {
        when(cinemaMapper.selectMissingCovers()).thenReturn(List.of(
                new CinemaMapper.MissingCoverTarget(1, "poi-a"),
                new CinemaMapper.MissingCoverTarget(2, "poi-b")));
        when(amapClient.firstPhotoUrl("poi-a")).thenReturn(Optional.empty());
        when(amapClient.firstPhotoUrl("poi-b")).thenReturn(Optional.of("http://img/b.jpg"));
        when(amapClient.downloadImage("http://img/b.jpg")).thenReturn(Optional.of(new byte[]{9}));
        doAnswer(invocation -> {
            invocation.getArgument(0, java.util.function.Consumer.class).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        assertThat(service().backfillMissingCovers()).isEqualTo(1);

        verify(cinemaMapper).updateAvatar(eq(2), org.mockito.ArgumentMatchers.startsWith("/files/"));
    }
}
