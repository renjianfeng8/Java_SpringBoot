package com.example.springboot.common;

import lombok.Data;

import com.example.springboot.common.enums.ErrorCode;

// 统一返回包装类
@Data
public class Result {
    private String code;
    private String msg;
    private Object data;

    public static Result success(){
        Result result = new Result();
        result.setCode("200");
        result.setMsg("请求成功");
        return result;
    }

    public static Result success(Object data){
        Result result = success();
        result.setData(data);
        return result;
    }

    public static Result error(){
        Result result = new Result();
        result.setCode(ErrorCode.SYSTEM_ERROR.code());
        result.setMsg(ErrorCode.SYSTEM_ERROR.message());
        return result;
    }

    public static Result error(String code, String msg){
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
