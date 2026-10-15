package com.example.springboot.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 影院实体类
 * 继承自Account类，使用Lombok注解简化代码
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cinema extends Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private String avatar;
    private String address;
    private String leader;
    private String code;
    private String certificate;
    private String status;
    private String phone;
    private String description;
    /** 关联的影院名录 id（可空）；区域经它 JOIN 取得 */
    private Integer directoryId;
    /** 以下三者不由 cinema 表存储，查询时 JOIN cinema_directory 带出，只读 */
    private String province;
    private String city;
    private String district;
}
