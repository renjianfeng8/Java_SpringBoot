package com.example.springboot.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 账户实体类
 * 使用Lombok注解简化getter、setter和构造方法
 */
@Data // 生成getter、setter、toString、equals、hashCode等方法
@NoArgsConstructor // 生成无参构造方法
@AllArgsConstructor // 生成全参构造方法
public class Account {
    private Integer id;
    private String username;
    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;
    private String role;
    private String name;
    private String newPassword;
    private String token; // JWT令牌
}
