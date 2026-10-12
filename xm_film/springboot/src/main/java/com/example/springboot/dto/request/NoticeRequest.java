package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 通知请求参数，字段与实体 Notice 一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeRequest {
    private Integer id;
    private String title;
    private String content;
    private String time;
}
