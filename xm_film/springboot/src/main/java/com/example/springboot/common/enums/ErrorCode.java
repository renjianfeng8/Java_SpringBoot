package com.example.springboot.common.enums;

public enum ErrorCode {
    UNAUTHORIZED("401", "登录状态已过期，请重新登录"),
    FORBIDDEN("403", "权限不足"),
    PARAM_INVALID("400", "参数校验失败"),
    NOT_FOUND("404", "资源不存在"),
    BUSINESS_CONFLICT("409", "业务状态冲突"),
    FILE_INVALID("415", "上传文件非法"),
    SYSTEM_ERROR("500", "系统错误");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
