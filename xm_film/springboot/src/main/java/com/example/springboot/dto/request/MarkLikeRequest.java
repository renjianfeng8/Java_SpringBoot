package com.example.springboot.dto.request;

import lombok.Data;

/**
 * 点赞入参。
 *
 * {@code liked} 是**显式意图**而非服务端 toggle：连点、重试、超时重发都收敛到用户
 * 想要的那个状态，不会来回翻转。为 null（未传）时控制器拒绝，而不是默认成"取消"。
 */
@Data
public class MarkLikeRequest {

    private Boolean liked;
}
