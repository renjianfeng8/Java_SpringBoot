package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 影片下拉选项。刻意不复用 FilmView：后者会把 content 等十几个 null 字段一并序列化，
 * 65 部片实测 19.9KB；只回 id/title 约 2KB。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilmOptionView {
    private Integer id;
    private String title;
}
