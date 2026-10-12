package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 预告片响应视图，字段与实体 Video 一致。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoView {
    private Integer id;
    private String title;
    private String img;
    private String name;
    private String preview;
    private String start;
}
