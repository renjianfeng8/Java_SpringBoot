package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Video implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String title;
    private String img;
    private String name;
    private String preview;
    private String start;
}
