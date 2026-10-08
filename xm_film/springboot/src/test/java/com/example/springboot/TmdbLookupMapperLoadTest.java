package com.example.springboot;

import com.example.springboot.entity.Actor;
import com.example.springboot.entity.Area;
import com.example.springboot.entity.Type;
import com.example.springboot.mapper.ActorMapper;
import com.example.springboot.mapper.AreaMapper;
import com.example.springboot.mapper.TypeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 绑定层守卫：`mvn compile` 不解析 mapper XML，语句 id 写错、#{参数名} 写错都能编译通过，
 * 只有真正执行才暴露。这里把 TMDB 导入用到的两条 type/area 语句各跑一遍。
 * <p>
 * 判重查询传一个不可能存在的标题，证明绑定有效且不触碰真实数据；
 * 自增 id 回填必须真插一行，用 finally 立即删除，保证可反复运行、不留痕。
 */
@SpringBootTest
@ActiveProfiles("ci")
class TmdbLookupMapperLoadTest {

    private static final String PROBE_TITLE = "__tmdb_import_probe__";

    @Autowired
    TypeMapper typeMapper;

    @Autowired
    AreaMapper areaMapper;

    @Autowired
    ActorMapper actorMapper;

    @Test
    void typeExactLookupReturnsNullWhenAbsent() {
        assertThat(typeMapper.selectByTitleExact(PROBE_TITLE)).isNull();
    }

    @Test
    void areaExactLookupReturnsNullWhenAbsent() {
        assertThat(areaMapper.selectByTitleExact(PROBE_TITLE)).isNull();
    }

    /**
     * 导入靠回填的自增 id 把新类型填进表单 typeIds，拿不到 id 就会静默回填空数组，
     * 表现为「导入成功了但类型没填上」。本用例钉住 id 确实被回填。
     */
    @Test
    void typeInsertBackfillsGeneratedId() {
        Type type = new Type(null, PROBE_TITLE);
        try {
            typeMapper.insert(type);

            assertThat(type.getId()).isNotNull();
            assertThat(typeMapper.selectByTitleExact(PROBE_TITLE).getId()).isEqualTo(type.getId());
        } finally {
            if (type.getId() != null) {
                typeMapper.deleteById(type.getId());
            }
        }
    }

    @Test
    void areaInsertBackfillsGeneratedId() {
        Area area = new Area(null, PROBE_TITLE);
        try {
            areaMapper.insert(area);

            assertThat(area.getId()).isNotNull();
            assertThat(areaMapper.selectByTitleExact(PROBE_TITLE).getId()).isEqualTo(area.getId());
        } finally {
            if (area.getId() != null) {
                areaMapper.deleteById(area.getId());
            }
        }
    }

    /**
     * 演职人员行同理：拿不到自增 id，导入就无法把 actorId 回填给表单，
     * film.actor_id 会恒为 null、前台演职人员区永远是空的。
     */
    @Test
    void actorInsertBackfillsGeneratedId() {
        Actor actor = new Actor();
        actor.setActorName(PROBE_TITLE);
        // actor.title 是 NOT NULL 且没有默认值 —— 导入路径必须把影片名一并写进去，
        // 缺这一项插入会被数据库直接拒绝（这条约束就靠本用例钉住）
        actor.setTitle(PROBE_TITLE);
        try {
            actorMapper.insert(actor);

            assertThat(actor.getId()).isNotNull();
            Actor saved = actorMapper.selectById(actor.getId());
            assertThat(saved.getActorName()).isEqualTo(PROBE_TITLE);
            assertThat(saved.getTitle()).isEqualTo(PROBE_TITLE);
        } finally {
            if (actor.getId() != null) {
                actorMapper.deleteById(actor.getId());
            }
        }
    }
}
