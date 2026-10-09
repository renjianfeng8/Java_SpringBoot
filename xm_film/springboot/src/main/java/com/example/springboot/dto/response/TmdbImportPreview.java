package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 从 TMDB 导入的**预填值**。字段名刻意与影片表单的 form 字段一一对应，
 * 前端拿到即可 {@code Object.assign(form, preview)}，不必再做一层翻译。
 *
 * 这里是草稿不是结果：管理员确认后仍走既有的 {@code POST /api/v1/films} 落库，
 * 所以本对象不携带任何 film 主键，也不代表数据库里已经多了一部影片。
 *
 * 图片一律是**本地** {@code /files/} 地址（下载成功时）：下载失败则为 null 并附一条 warning，
 * 绝不下发 TMDB 的远程地址 —— 否则 film.img 会同时存在本地与远程两种形态。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TmdbImportPreview {

    private String title;
    private String english;
    private String start;
    private Integer time;
    private String language;
    private String content;
    private String img;
    private String employee;
    private String status;

    /** 预告片的 YouTube 嵌入地址（TMDB 只给 YouTube 视频 id，播放走 iframe）；该片没有可取用的则 null */
    private String video;

    /** 已复用或新建的 region，可直接填进表单的 areaId */
    private Integer areaId;

    /** areaId 对应的中文名，供弹窗回显 —— 前端不必先去刷一次地区列表 */
    private String areaName;

    /** 已新建的演职人员行（首位主演） */
    private Integer actorId;

    /** actorId 对应的演员名，供弹窗回显 */
    private String actorName;

    /** 已复用或新建的类型 id，已截到表单上限（4 个） */
    private List<Integer> typeIds;

    /** 降级说明：某项没取到、被截断、下载失败等。有值时前端应提示管理员复核。 */
    private List<String> warnings;
}
