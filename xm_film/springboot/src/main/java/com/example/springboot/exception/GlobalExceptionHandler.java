package com.example.springboot.exception;

import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientException;

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
    public Result<Void> validationError(MethodArgumentNotValidException e) {
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
    public Result<Void> error(CustomException e) {
        logger.warn("业务异常: code={}, message={}", e.getCode(), e.getMsg(), e);
        return Result.error(e.getCode(), e.getMsg());
    }

    /**
     * 处理外部接口请求失败（TMDB 经本机代理访问，代理未开即此异常）。
     * @param e 外部接口异常对象
     * @return 返回可操作的提示，提示管理员确认本机代理
     */
    @ExceptionHandler(RestClientException.class)
    @ResponseBody
    public Result<Void> restClientError(RestClientException e) {
        logger.warn("外部接口请求失败: {}", e.getMessage());
        return Result.error(ErrorCode.SYSTEM_ERROR.code(), "TMDB 接口不可达，请确认本机代理已开启");
    }

    /**
     * 处理所有未捕获的Exception异常
     * @param e 异常对象
     * @return 返回包含错误信息的统一响应格式
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result<Void> error(Exception e) {
        logger.error("系统异常:", e);
        return Result.error();
    }
}