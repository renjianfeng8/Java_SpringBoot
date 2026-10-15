package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 影片请求参数，字段与实体 Film（含查询与展示字段）一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilmRequest {
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
    private Integer typeId;
    private Double boxOffice;
    private Integer actorId;
    private String actorInfo;
    private Integer topNum;
    private String video;
    private List<Integer> typeIds;
}
