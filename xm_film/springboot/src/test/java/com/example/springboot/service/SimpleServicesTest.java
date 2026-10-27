package com.example.springboot.service;

import com.example.springboot.entity.Actor;
import com.example.springboot.entity.Area;
import com.example.springboot.entity.Notice;
import com.example.springboot.entity.Video;
import com.example.springboot.mapper.ActorMapper;
import com.example.springboot.mapper.AreaMapper;
import com.example.springboot.mapper.NoticeMapper;
import com.example.springboot.mapper.VideoMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 四个「纯 CRUD」服务：Area / Actor / Notice / Video。
 *
 * <p>它们没有业务分支，测试价值不在重复 BaseService 的行为，而在于确认
 * <b>每个子类绑对了自己的 mapper</b> —— 这类错配编译期发现不了，只会表现为
 * 「保存成功但数据写进了另一张表」。
 */
class SimpleServicesTest {

    @Test
    void areaServiceUsesAreaMapper() {
        AreaMapper mapper = mock(AreaMapper.class);
        AreaService service = new AreaService(mapper);
        Area area = new Area();
        area.setTitle("中国大陆");

        service.add(area);

        verify(mapper).insert(area);
    }

    @Test
    void actorServiceUsesActorMapper() {
        ActorMapper mapper = mock(ActorMapper.class);
        ActorService service = new ActorService(mapper);
        Actor actor = new Actor();
        actor.setActorName("吴京");

        service.add(actor);

        verify(mapper).insert(actor);
    }

    @Test
    void noticeServiceUsesNoticeMapper() {
        NoticeMapper mapper = mock(NoticeMapper.class);
        NoticeService service = new NoticeService(mapper);
        Notice notice = new Notice();
        notice.setTitle("系统公告");

        service.add(notice);

        verify(mapper).insert(notice);
    }

    @Test
    void videoServiceUsesVideoMapper() {
        VideoMapper mapper = mock(VideoMapper.class);
        VideoService service = new VideoService(mapper);
        Video video = new Video();
        video.setTitle("预告片");
        when(mapper.selectAll(video)).thenReturn(List.of(video));

        service.add(video);

        verify(mapper).insert(video);
        assertThat(service.selectAll(video)).containsExactly(video);
    }
}
