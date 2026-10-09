package com.example.springboot;

import com.example.springboot.common.CinemaBrand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CinemaBrandTest {

    @Test
    void matchesKnownChainByKeyword() {
        assertThat(CinemaBrand.derive("万达影城(通州万达广场店)")).isEqualTo("万达影城");
        assertThat(CinemaBrand.derive("横店电影城(西单店)")).isEqualTo("横店影视");
        assertThat(CinemaBrand.derive("大地影院(望京店)")).isEqualTo("大地影院");
    }

    /** ASCII 品牌名大小写不敏感 */
    @Test
    void matchesAsciiBrandCaseInsensitively() {
        assertThat(CinemaBrand.derive("cgv影城(奥体店)")).isEqualTo("CGV影城");
        assertThat(CinemaBrand.derive("ume国际影城")).isEqualTo("UME影城");
    }

    /** 具体品牌优先于其子串："橙天嘉禾" 必须先于 "嘉禾" 命中 */
    @Test
    void moreSpecificBrandWinsOverSubstring() {
        assertThat(CinemaBrand.derive("橙天嘉禾影城(合生汇店)")).isEqualTo("橙天嘉禾");
    }

    @Test
    void unmatchedReturnsNullAndNullIsSafe() {
        assertThat(CinemaBrand.derive("某某私人影院")).isNull();
        assertThat(CinemaBrand.derive(null)).isNull();
    }
}
