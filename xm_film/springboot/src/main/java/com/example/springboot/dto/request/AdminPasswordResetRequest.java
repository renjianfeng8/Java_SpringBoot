package com.example.springboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员重置他人登录密码。账号角色决定去 admin/user/cinema 哪张表。 */
@Data
public class AdminPasswordResetRequest {

    @NotNull(message = "账号 ID 不能为空")
    private Integer id;

    @NotBlank(message = "账号角色不能为空")
    private String role;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度需为 6-64 位")
    private String newPassword;
}
