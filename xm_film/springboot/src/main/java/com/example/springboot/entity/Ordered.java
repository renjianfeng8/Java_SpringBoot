package com.example.springboot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ordered {
    private Integer id;
    private String orders;
    private Integer recordId;
    private Integer userId;
    private Integer filmId;
    private String img;
    private Integer cinemaId;
    private Integer roomId;
    private String appointment;
    private double total;
    private Integer number;
    private String status;
    private String start;
    private String seat;

    private String userName;
    private String filmName;
    private String cinemaName;
    private String roomName;
}
