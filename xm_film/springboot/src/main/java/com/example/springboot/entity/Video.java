package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    private Integer id;
    private String title;
    private String img;
    private String name;
    private String preview;
    private String start;
}
