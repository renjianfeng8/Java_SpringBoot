package com.example.springboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateRequest {
    @NotNull(message = "放映场次不能为空")
    private Integer recordId;

    @NotBlank(message = "座位不能为空")
    private String seat;
}
