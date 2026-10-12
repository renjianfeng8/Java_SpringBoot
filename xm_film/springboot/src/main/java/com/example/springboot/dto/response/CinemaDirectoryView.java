package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 影院名录响应视图，字段与实体 CinemaDirectory 一致（含派生 linked）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaDirectoryView {
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
