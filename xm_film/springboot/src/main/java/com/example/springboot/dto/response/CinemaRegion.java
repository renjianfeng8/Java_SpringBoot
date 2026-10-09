package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 前台区域级联项：一个城市及其下已含影院的区县（区县可为空表） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaRegion {
    private String city;
    private List<String> districts;
}
