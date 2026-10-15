package com.example.springboot.entity;

import com.example.springboot.common.Identifiable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Type implements Serializable, Identifiable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String title;
}