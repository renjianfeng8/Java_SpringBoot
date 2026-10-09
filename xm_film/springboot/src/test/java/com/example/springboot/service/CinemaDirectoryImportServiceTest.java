package com.example.springboot.service;

import com.example.springboot.common.AmapClient;
import com.example.springboot.dto.response.CinemaImportPreview;
import com.example.springboot.entity.CinemaDirectory;
import com.example.springboot.mapper.CinemaDirectoryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

    private static AmapClient.CinemaPoi poi(String id, String name, String city) {
        return new AmapClient.CinemaPoi(id, name, "北京市", city, "通州区", "某路1号", "010-1");
    }

    private CinemaDirectoryImportService service(AmapClient client, CinemaDirectoryMapper mapper) {
        return new CinemaDirectoryImportService(client, mapper);
    }

    @Test
    void previewCountsFreshVersusDuplicateAndDoesNotInsert() {
        AmapClient client = mock(AmapClient.class);
        CinemaDirectoryMapper mapper = mock(CinemaDirectoryMapper.class);
        when(client.searchCinemas(eq("北京"), eq(1), anyInt())).thenReturn(
                new AmapClient.PoiPage(2, List.of(poi("A", "万达影城(a)", "北京市"),
                        poi("B", "CGV影城(b)", "北京市"))));
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
                new AmapClient.PoiPage(1, List.of(poi("A", "万达影城(a)", "北京市"))));
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

    /** count > 200：必须按区县细分，各区县各自抓，而不是只翻到 200 条就停 */
    @Test
    void subdividesByDistrictWhenCityExceedsTwoHundred() {
        AmapClient client = mock(AmapClient.class);
        CinemaDirectoryMapper mapper = mock(CinemaDirectoryMapper.class);
        when(client.searchCinemas(eq("北京"), eq(1), anyInt()))
                .thenReturn(new AmapClient.PoiPage(300, List.of(poi("A", "万达影城(a)", "北京市"))));
        when(client.districts("北京")).thenReturn(List.of(
                new AmapClient.District("东城区", "110101"),
                new AmapClient.District("通州区", "110112")));
        when(client.searchCinemas(eq("110101"), eq(1), anyInt()))
                .thenReturn(new AmapClient.PoiPage(1, List.of(poi("D1", "影院1", "北京市"))));
        when(client.searchCinemas(eq("110112"), eq(1), anyInt()))
                .thenReturn(new AmapClient.PoiPage(1, List.of(poi("D2", "影院2", "北京市"))));
        when(mapper.selectExistingPoiIds(anyList())).thenReturn(List.of());

        CinemaImportPreview preview = service(client, mapper).preview(List.of("北京"));

        // 城市级那次只用于读 count，真正入库的两个区县 POI 才计数
        assertThat(preview.getTotal()).isEqualTo(2);
        verify(client).districts("北京");
    }
}
