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
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer cinemaId;
    private String title;
    private String name;
    private Integer seatRows;
    private Integer seatCols;
}
