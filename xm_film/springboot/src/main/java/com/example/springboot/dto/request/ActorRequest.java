package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 演员请求参数，字段与实体 Actor 一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActorRequest {
    private Integer id;
    private String title;
    private String img;
    private String actorName;
    private String figure;
    private String picture;
    private String grade;
    private String video;
}
