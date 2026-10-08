package com.example.springboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 设置 / 重设支付密码：验登录密码后写入。
 *
 * 「首次设置」与「忘记支付密码」是同一条路径 —— 两者都是"拿登录密码换一个新支付密码"，
 * 没有第二个语义，所以不拆两个端点。已设置过的用户想直接改，走
 * {@link PayPasswordChangeRequest}（验原支付密码）。
 */
@Data
public class PayPasswordResetRequest {

    @NotBlank(message = "请输入登录密码")
    private String loginPassword;

    @NotBlank(message = "请输入支付密码")
    @Pattern(regexp = "\\d{6}", message = "支付密码须为 6 位数字")
    private String payPassword;
}
