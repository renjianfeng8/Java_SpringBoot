package com.example.springboot.exception;

import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.stream.Collectors;

@ControllerAdvice("com.example.springboot.controller") //提供全局异常处理
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数校验失败异常
     * @param e 参数校验异常对象
     * @return 返回包含错误码400和详细字段错误信息的响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Result validationError(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        logger.warn("参数校验失败: {}", message);
        return Result.error(ErrorCode.PARAM_INVALID.code(), message);
    }

    /**
     * 处理自定义业务异常CustomException
     * @param e 自定义异常对象
     * @return 返回包含自定义错误码和消息的响应
     */
    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public Result error(CustomException e) {
        logger.warn("业务异常: code={}, message={}", e.getCode(), e.getMsg(), e);
        return Result.error(e.getCode(), e.getMsg());
    }

    /**
     * 处理所有未捕获的Exception异常
     * @param e 异常对象
     * @return 返回包含错误信息的统一响应格式
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result error(Exception e) {
        logger.error("系统异常:", e);
        return Result.error();
    }
}