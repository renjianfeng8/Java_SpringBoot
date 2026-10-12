package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 通知响应视图，字段与实体 Notice 一致。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeView {
    private Integer id;
    private String title;
    private String content;
    private String time;
}
