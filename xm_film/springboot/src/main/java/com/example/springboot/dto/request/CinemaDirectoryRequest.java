package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 影院名录请求参数，字段与实体 CinemaDirectory（含派生 linked 筛选字段）一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaDirectoryRequest {
    private Integer id;
    private String poiId;
    private String name;
    private String brand;
    private String province;
    private String city;
    private String district;
    private String address;
    private String phone;
    private String source;
    private LocalDateTime createTime;
    private Boolean linked;
}
