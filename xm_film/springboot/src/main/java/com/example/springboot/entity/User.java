package com.example.springboot.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends Account {
    private Integer id;
    private String username;
    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;
    private String role;
    private String name;
    private String avatar;
    private String phone;
    private String email;
}
