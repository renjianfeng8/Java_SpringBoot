package com.example.springboot.dto.response;

import lombok.Data;

/**
 * 取票大厅的出票凭条。
 *
 * 这个端点**匿名可读** —— 核销的授权凭证是取票码本身，不是登录身份，所以响应体必须
 * 只包含"出的是什么票"：谁持有码谁就能看到这些字段，这是设计如此。
 * 因此刻意不放 orderId、订单编号、金额、userId —— 那些属于订单明细，
 * 只该由持有登录态的本人在 /api/v1/orders 下看到（同 SeatOccupancy 的口径）。
 */
@Data
public class TicketVoucher {

    private String filmTitle;
    private String cinemaName;
    private String roomName;

    /** 放映时间，与 ordered.start 同形态（yyyy-MM-dd HH:mm:ss） */
    private String start;

    private String seat;
    private Integer number;
}
