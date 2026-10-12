package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 排片请求参数，字段与实体 Record（含 JOIN 展示字段）一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordRequest {
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
