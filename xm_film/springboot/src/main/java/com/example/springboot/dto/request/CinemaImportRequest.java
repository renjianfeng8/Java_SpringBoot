package com.example.springboot.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CinemaImportRequest {
    /** 要导入的城市名列表（与高德 region 可传的城市名一致） */
    private List<String> cities;
}
