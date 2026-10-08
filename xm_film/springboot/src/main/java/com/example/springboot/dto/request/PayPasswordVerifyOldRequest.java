package com.example.springboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 设置页第一步：只验原支付密码，不写库（对应 {@link PayPasswordChangeRequest} 的验证环节） */
@Data
public class PayPasswordVerifyOldRequest {

    @NotBlank(message = "请输入原支付密码")
    private String oldPassword;
}
