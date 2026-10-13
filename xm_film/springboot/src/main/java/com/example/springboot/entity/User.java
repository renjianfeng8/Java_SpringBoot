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
public class User extends Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private String avatar;
    private String phone;
    private String email;
}
