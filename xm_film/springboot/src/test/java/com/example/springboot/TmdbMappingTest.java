package com.example.springboot;

import com.example.springboot.common.TmdbMapping;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TmdbMappingTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

    // ========== 语言码 → 表单的 7 个选项 ==========

    @Test
    void mapsCoveredLanguageCodesToFormOptions() {
        assertThat(TmdbMapping.languageFormOption("zh")).isEqualTo("普通话");
        assertThat(TmdbMapping.languageFormOption("cn")).isEqualTo("港语");
        assertThat(TmdbMapping.languageFormOption("en")).isEqualTo("英语");
        assertThat(TmdbMapping.languageFormOption("fr")).isEqualTo("法语");
        assertThat(TmdbMapping.languageFormOption("ja")).isEqualTo("日语");
        assertThat(TmdbMapping.languageFormOption("ru")).isEqualTo("俄语");
    }

    @Test
    void unmappedLanguageFallsBackToOther() {
        // 表单只有 7 个选项，韩语/德语/西班牙语没有对应项，归入「其他」而不是留空
        assertThat(TmdbMapping.languageFormOption("ko")).isEqualTo("其他");
        assertThat(TmdbMapping.languageFormOption("de")).isEqualTo("其他");
        assertThat(TmdbMapping.languageFormOption("es")).isEqualTo("其他");
    }

    @Test
    void missingLanguageLeavesBlank() {
        // 缺失时不猜「其他」—— 留空让必填校验拦下来
        assertThat(TmdbMapping.languageFormOption(null)).isEmpty();
        assertThat(TmdbMapping.languageFormOption("")).isEmpty();
        assertThat(TmdbMapping.languageFormOption("   ")).isEmpty();
    }

    @Test
    void languageCodeIsCaseInsensitive() {
        assertThat(TmdbMapping.languageFormOption("EN")).isEqualTo("英语");
    }

    // ========== 上映日期 → 影片状态 ==========

    @Test
    void pastReleaseDateMeansReleased() {
        assertThat(TmdbMapping.deriveStatus("1999-10-15", TODAY)).isEqualTo("已上映");
    }

    @Test
    void sameDayReleaseCountsAsReleased() {
        assertThat(TmdbMapping.deriveStatus("2026-10-08", TODAY)).isEqualTo("已上映");
    }

    @Test
    void futureReleaseDateMeansUpcoming() {
        assertThat(TmdbMapping.deriveStatus("2026-12-25", TODAY)).isEqualTo("待上映");
    }

    @Test
    void missingOrUnparseableDateLeavesBlank() {
        // TMDB 对未定档影片返回空串；解析不了也不猜，留空交必填校验
        assertThat(TmdbMapping.deriveStatus(null, TODAY)).isEmpty();
        assertThat(TmdbMapping.deriveStatus("", TODAY)).isEmpty();
        assertThat(TmdbMapping.deriveStatus("not-a-date", TODAY)).isEmpty();
    }

    // ========== 类型数量上限 ==========

    @Test
    void trimsGenresToFormLimit() {
        // 表单 handleTypeChange 硬性最多 4 个类型，导入时先截断
        assertThat(TmdbMapping.trimToFormLimit(List.of(1, 2, 3, 4, 5, 6))).containsExactly(1, 2, 3, 4);
    }

    @Test
    void keepsGenresWithinLimit() {
        assertThat(TmdbMapping.trimToFormLimit(List.of(1, 2))).containsExactly(1, 2);
    }

    @Test
    void emptyGenresStayEmpty() {
        assertThat(TmdbMapping.trimToFormLimit(List.of())).isEmpty();
        assertThat(TmdbMapping.trimToFormLimit(null)).isEmpty();
    }
}
