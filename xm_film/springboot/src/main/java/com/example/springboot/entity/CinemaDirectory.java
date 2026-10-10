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
    /**
     * 是否已被 cinema 收录（存在引用本行 id 的影院）。查询时派生，非本表列；
     * 同时充当筛选入参：null=全部，true=已收录，false=未收录。
     */
    private Boolean linked;
}
