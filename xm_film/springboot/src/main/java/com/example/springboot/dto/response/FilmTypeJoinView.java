package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * film_type 关联的一行：某影片挂载的类型（供列表页一次性批量填充类型详情，避免逐条影片各查一轮）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilmTypeJoinView {
    private Integer filmId;
    private Integer typeId;
    private String typeTitle;
}
