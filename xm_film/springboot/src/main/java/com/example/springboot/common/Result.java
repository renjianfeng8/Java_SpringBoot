package com.example.springboot.common;

import com.example.springboot.common.enums.ErrorCode;
import lombok.Data;

// 统一返回包装类
@Data
public class Result<T> {
    private String code;
    private String msg;
    private T data;

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode("200");
        result.setMsg("请求成功");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = success();
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error() {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SYSTEM_ERROR.code());
        result.setMsg(ErrorCode.SYSTEM_ERROR.message());
        return result;
    }

    public static <T> Result<T> error(String code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
