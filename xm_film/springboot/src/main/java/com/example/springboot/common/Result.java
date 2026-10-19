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
        result.setCode(SuccessCode.VALUE);
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

    /**
     * 以错误码构造失败响应。文案由调用点给出（同一种错误码常配不同文案），
     * 与 {@link #error(String, String)} 只差"帮调用方取一次 code()"。
     *
     * <p>存在的理由：没有它时全仓要写 {@code Result.error(ErrorCode.X, msg)}，
     * 每个调用点都把"错误码的字符串形态"翻出来一次 —— 那本应是 {@link ErrorCode} 的内部细节。
     */
    public static <T> Result<T> error(ErrorCode errorCode, String msg) {
        return error(errorCode.code(), msg);
    }
}
