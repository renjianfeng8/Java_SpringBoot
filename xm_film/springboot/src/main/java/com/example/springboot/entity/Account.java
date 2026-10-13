package com.example.springboot.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 账户实体类
 * 使用Lombok注解简化getter、setter和构造方法
 */
@Getter // 生成getter方法
@Setter // 生成setter方法
@NoArgsConstructor // 生成无参构造方法
@AllArgsConstructor // 生成全参构造方法
public class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String username;
    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;
    private String role;
    private String name;
    @JsonProperty(access = Access.WRITE_ONLY)
    private String newPassword;
    private String token; // JWT令牌
}
