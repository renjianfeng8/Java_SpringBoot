package com.example.springboot.exception;

import com.example.springboot.common.enums.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomException extends RuntimeException {
    private String code;
    private String msg;

    public CustomException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public CustomException(ErrorCode errorCode) {
        this(errorCode.code(), errorCode.message());
    }

    public CustomException(ErrorCode errorCode, String msg) {
        this(errorCode.code(), msg);
    }
}
