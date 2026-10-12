package com.example.springboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 影厅请求参数，字段与实体 Room 一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {
    private Integer id;
    private Integer cinemaId;
    private String title;
    private String name;
    private Integer seatRows;
    private Integer seatCols;
}
