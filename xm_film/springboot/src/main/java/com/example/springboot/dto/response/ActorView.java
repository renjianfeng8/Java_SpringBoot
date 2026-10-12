package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 演员响应视图，字段与实体 Actor 一致。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActorView {
    private Integer id;
    private String title;
    private String img;
    private String actorName;
    private String figure;
    private String picture;
    private String grade;
    private String video;
}
