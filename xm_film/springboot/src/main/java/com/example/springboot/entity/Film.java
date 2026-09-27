package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Film {
  private Integer id;
  private String title;
  private String english;
  private String start;
  private Integer time;
  private String language;
  private String resolution;
  private String content;
  private String img;
  private String employee;
  private Integer areaId;
  private String status;
  private Double score;
  private String year;
  private String areaName;
  private List<Integer> ids;
  private Integer typeId;
  // 必须用包装类型：FilmMapper.updateById 用 <if test="boxOffice != null"> 守卫，
  // 原始类型 double 永远非 null，任何不带票房的局部更新都会把票房写成 0（同 BUG-034）
  private Double boxOffice;
  private Integer actorId;
  private String actorInfo;
  private Integer topNum;
  private String video;
  private List<Integer> typeIds;
  private List<Type> typeList;
}
