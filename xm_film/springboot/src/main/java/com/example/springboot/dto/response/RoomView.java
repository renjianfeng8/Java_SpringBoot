package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 影厅响应视图，字段与实体 Room 一致。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomView {
    private Integer id;
    private Integer cinemaId;
    private String title;
    private String name;
    private Integer seatRows;
    private Integer seatCols;
}
