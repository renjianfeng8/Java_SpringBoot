package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Record implements Serializable {
    private static final long serialVersionUID = 1L;

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
    /** 所属影厅的座位行列数，供前台选座图渲染（/api/v1/records 匿名可读，用户端无需再暴露影厅接口） */
    private Integer roomSeatRows;
    private Integer roomSeatCols;
}
