package com.example.springboot.common.enums;

public final class RecordStatus {
    private RecordStatus() {}

    /** 正常 — 到点前可正常售票 */
    public static final String NORMAL = "正常";

    /** 停售 — 人工下架该场次，不再售票 */
    public static final String STOPPED = "停售";
}
