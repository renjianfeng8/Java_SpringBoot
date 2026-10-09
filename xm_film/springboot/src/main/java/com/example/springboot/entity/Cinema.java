package com.example.springboot.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * 影院实体类
 * 继承自Account类，使用Lombok注解简化代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true) // 调用父类的equals和hashCode方法
public class Cinema extends Account {
    private String avatar;
    private String email;
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
