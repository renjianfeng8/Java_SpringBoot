package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 前台区域级联的原始一行：某个城市下的一个区县（area 为空表时 district 可为 null）。
 * 与 {@link CinemaRegionView} 的区别：那是按城市归组后的树，本项是归组前的扁平行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaRegionItemView {
    private String city;
    private String district;
}
