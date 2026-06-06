package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Record {
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
}
