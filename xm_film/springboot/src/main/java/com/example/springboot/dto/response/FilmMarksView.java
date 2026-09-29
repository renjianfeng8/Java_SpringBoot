package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 某部影片的评价页数据：当前页 + 本人评价 + 发表资格。
 *
 * 热评不是另一条查询 —— 影片详情页的 Top3 就是 {@code list} 这条
 * 「赞数降序 → id 降序」排序的头部。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilmMarksView {

    /** 该片评价总数（含本人），由 PageInfo 给出 */
    private long total;

    /** 当前 USER 是否够格发表评价（= 对该片有已取票订单）；是否**已**评过看 my 是否为 null */
    private boolean reviewable;

    /** 本人对该片的评价，未评过为 null */
    private MarkView my;

    /** 当前页评价列表 */
    private List<MarkView> list;
}
