package com.example.springboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 修改支付密码：先验原支付密码，再写入新密码 */
@Data
public class PayPasswordChangeRequest {

    @NotBlank(message = "请输入原支付密码")
    private String oldPassword;

    @NotBlank(message = "请输入新支付密码")
    @Pattern(regexp = "\\d{6}", message = "支付密码须为 6 位数字")
    private String payPassword;
}
