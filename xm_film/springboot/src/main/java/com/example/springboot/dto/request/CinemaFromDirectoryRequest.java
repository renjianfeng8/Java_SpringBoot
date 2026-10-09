package com.example.springboot.dto.request;

import lombok.Data;

import java.util.List;

/** 「从名录生成影院」请求：被勾选的名录 id */
@Data
public class CinemaFromDirectoryRequest {
    private List<Integer> directoryIds;
}
