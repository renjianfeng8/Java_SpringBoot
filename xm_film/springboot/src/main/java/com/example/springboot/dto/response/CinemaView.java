package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 影院响应视图，剔除 password、newPassword、token。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaView {
    private Integer id;
    private String username;
    private String role;
    private String name;
    private String avatar;
    private String address;
    private String status;
    private String phone;
    private String description;
    private Integer directoryId;
    private String province;
    private String city;
    private String district;
}
