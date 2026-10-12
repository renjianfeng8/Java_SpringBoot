package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 预告片请求参数，字段与实体 Video 一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoRequest {
    private Integer id;
    private String title;
    private String img;
    private String name;
    private String preview;
    private String start;
}
