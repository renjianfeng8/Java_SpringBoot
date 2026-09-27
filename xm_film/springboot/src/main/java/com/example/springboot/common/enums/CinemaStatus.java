package com.example.springboot.common.enums;

/**
 * 影院审核状态词表 —— 与 schema 默认值、data.sql 种子数据保持同一套取值。
 * 公开接口只放行「已审核」的影院；「未审核」既不可登录，也不对外展示。
 */
public final class CinemaStatus {
    private CinemaStatus() {}

    /** 未审核 — 新注册/新建影院的初始状态，管理员审核通过后才对外可见 */
    public static final String UNAUDITED = "未审核";

    /** 已审核 — 通过审核，对前台可见且可登录 */
    public static final String APPROVED = "已审核";
}
