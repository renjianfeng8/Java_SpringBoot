package com.example.springboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 取票大厅核销入参。
 *
 * **刻意只有取票码，没有 orderId。** 如果允许传 orderId，任何人都能核销别人的订单 ——
 * 那么"码"就不再是凭证、退化成装饰。码本身即授权，这是这个端点的全部安全模型。
 */
@Data
public class TicketRedeemRequest {

    @NotBlank(message = "请输入取票码")
    private String code;
}
