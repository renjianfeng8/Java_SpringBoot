package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 影片响应视图，字段与实体 Film 一致（含类型与票房展示字段）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilmView {
    private Integer id;
    private String title;
    private String english;
    private String start;
    private Integer time;
    private String language;
    private String content;
    private String img;
    private String employee;
    private Integer areaId;
    private String status;
    private Double score;
    private String year;
    private String areaName;
    private Double boxOffice;
    private Integer actorId;
    private String actorInfo;
    private Integer topNum;
    private String video;
    private List<Integer> typeIds;
    private List<TypeView> typeList;
}
