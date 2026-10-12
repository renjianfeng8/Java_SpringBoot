package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 排片响应视图，字段与实体 Record 一致（含 JOIN 展示字段）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordView {
    private Integer id;
    private Integer cinemaId;
    private Integer roomId;
    private Integer filmId;
    private String title;
    private String start;
    private String price;
    private String status;
    private String cinemaName;
    private String roomName;
    private Integer roomSeatRows;
    private Integer roomSeatCols;
}
