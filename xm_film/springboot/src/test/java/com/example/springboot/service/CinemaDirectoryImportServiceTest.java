package com.example.springboot.service;

import com.example.springboot.common.AmapClient;
import com.example.springboot.dto.response.CinemaImportPreview;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CinemaDirectoryImportServiceTest {

    /** 高德每页最多 25 条；fetchCity 靠「本页是否满」判断有没有抓完 */
    private static final int PAGE_SIZE = 25;

    private static AmapClient.CinemaPoi poi(String id, String name, String city) {
        return new AmapClient.CinemaPoi(id, name, "北京市", city, "通州区", "某路1号", "010-1");
    }

    /** 一整页（25 条）POI，id 带页前缀保证跨页不重复 */
    private static List<AmapClient.CinemaPoi> fullPage(String prefix) {
        List<AmapClient.CinemaPoi> list = new ArrayList<>();
        for (int i = 0; i < PAGE_SIZE; i++) {
            list.add(poi(prefix + i, "影院" + i, "北京市"));
        }
        return list;
    }

    private CinemaDirectoryImportService service(AmapClient client, CinemaDirectoryMapper mapper) {
        return new CinemaDirectoryImportService(client, mapper);
    }

    @Test
    void previewCountsFreshVersusDuplicateAndDoesNotInsert() {
        AmapClient client = mock(AmapClient.class);
        CinemaDirectoryMapper mapper = mock(CinemaDirectoryMapper.class);
        when(client.searchCinemas(eq("北京"), eq(1), anyInt())).thenReturn(
                List.of(poi("A", "万达影城(a)", "北京市"), poi("B", "CGV影城(b)", "北京市")));
        when(mapper.selectExistingPoiIds(anyList())).thenReturn(List.of("A")); // A 已存在

        CinemaImportPreview preview = service(client, mapper).preview(List.of("北京"));

        assertThat(preview.getTotal()).isEqualTo(2);
        assertThat(preview.getFreshCount()).isEqualTo(1);
        assertThat(preview.getDuplicateCount()).isEqualTo(1);
        verify(mapper, never()).insertIgnore(any());
    }

    @Test
    void importInsertsOnlyFreshRowsWithDerivedBrand() {
        AmapClient client = mock(AmapClient.class);
        CinemaDirectoryMapper mapper = mock(CinemaDirectoryMapper.class);
        when(client.searchCinemas(eq("北京"), eq(1), anyInt())).thenReturn(
                List.of(poi("A", "万达影城(a)", "北京市")));
        when(mapper.selectExistingPoiIds(anyList())).thenReturn(List.of());
        when(mapper.insertIgnore(any())).thenReturn(1); // INSERT IGNORE 实际写入行数

        int inserted = service(client, mapper).importAll(List.of("北京"));

        ArgumentCaptor<CinemaDirectory> captor = ArgumentCaptor.forClass(CinemaDirectory.class);
        verify(mapper, times(1)).insertIgnore(captor.capture());
        assertThat(inserted).isEqualTo(1);
        assertThat(captor.getValue().getPoiId()).isEqualTo("A");
        assertThat(captor.getValue().getBrand()).isEqualTo("万达影城");
        assertThat(captor.getValue().getSource()).isEqualTo("amap");
    }

    /**
     * 城市级取满 8 页（= 200 硬上限）说明还有更多没取到：必须按区县细分重抓。
     * 高德 v5 不给总命中数（count 只是本页条数），所以只能靠「是否触顶」判断。
     */
    @Test
    void subdividesByDistrictWhenCityHitsTwoHundredCap() {
        AmapClient client = mock(AmapClient.class);
        CinemaDirectoryMapper mapper = mock(CinemaDirectoryMapper.class);
        // 城市级 8 页全满 = 200 条 → 触顶
        when(client.searchCinemas(eq("北京"), anyInt(), anyInt())).thenAnswer(inv -> {
            int page = inv.getArgument(1);
            return page <= 8 ? fullPage("C" + page + "-") : List.of();
        });
        when(client.districts("北京")).thenReturn(List.of(
                new AmapClient.District("东城区", "110101"),
                new AmapClient.District("通州区", "110112")));
        // 各区县不满一页 → 抓完即停
        when(client.searchCinemas(eq("110101"), eq(1), anyInt()))
                .thenReturn(List.of(poi("D1", "影院1", "北京市")));
        when(client.searchCinemas(eq("110112"), eq(1), anyInt()))
                .thenReturn(List.of(poi("D2", "影院2", "北京市")));
        when(mapper.selectExistingPoiIds(anyList())).thenReturn(List.of());

        CinemaImportPreview preview = service(client, mapper).preview(List.of("北京"));

        // 城市级那 200 条不参与计数，真正入库的两个区县 POI 才计数
        assertThat(preview.getTotal()).isEqualTo(2);
        verify(client).districts("北京");
    }
}
