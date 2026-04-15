package com.example.springboot.exception;

import com.example.springboot.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


@ControllerAdvice("com.example.springboot.controller") //提供全局异常处理
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理所有未捕获的Exception异常
     * @param e 异常对象
     * @return 返回包含错误信息的统一响应格式
     */
    @ExceptionHandler(Exception.class)  //全局异常处理
    @ResponseBody
    public Result error(Exception e) {
        // 使用ERROR级别记录异常堆栈
        logger.error("系统异常:", e);
        return Result.error();
    }

    /**
     * 处理自定义业务异常CustomException
     * @param e 自定义异常对象
     * @return 返回包含自定义错误码和消息的响应
     */
    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public Result error(CustomException e) {
        // 使用WARN级别记录业务异常（可根据需要调整为ERROR）
        logger.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }
}