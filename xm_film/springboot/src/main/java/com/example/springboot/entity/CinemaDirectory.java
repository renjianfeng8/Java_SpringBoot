package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 影院名录：高德 POI 导入的只读门店资料，与账号表 cinema 无关 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaDirectory {
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
}
