package com.example.springboot.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 模拟支付网关回调入参。正式环境里由支付平台按自己的结果码映射成 success，
 * 演示环境由前端「模拟回调成功 / 失败」两个按钮分别发 true / false。
 */
@Data
public class RechargeCallbackRequest {
    @NotNull(message = "回调结果不能为空")
    private Boolean success;

    /** 失败原因等备注，成功时可留空 */
    private String remark;
}
