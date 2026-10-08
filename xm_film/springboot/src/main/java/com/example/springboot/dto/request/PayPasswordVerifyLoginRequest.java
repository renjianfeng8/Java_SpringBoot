package com.example.springboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 设置页第一步：只验登录密码，不写库（对应 {@link PayPasswordResetRequest} 的验证环节） */
@Data
public class PayPasswordVerifyLoginRequest {

    @NotBlank(message = "请输入登录密码")
    private String loginPassword;
}
