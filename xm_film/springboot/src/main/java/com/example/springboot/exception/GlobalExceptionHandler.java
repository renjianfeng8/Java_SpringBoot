package com.example.springboot.exception;

import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把各类异常统一映射为 {@link Result} 外壳，前端只需看 {@code code}。
 *
 * <p><b>为什么用 {@code basePackages} 限定到 controller 包</b>：Spring 的处理器是
 * 「就近优先」的，限定之后 controller 自己的 {@code @ExceptionHandler} 仍然优先，
 * 而本类的 9 档映射只作用于 REST 接口，不会劫持框架自身的页面/资源请求。
 * 代价是它依赖一个<b>字符串包名</b>：一旦 controller 包被移动，本类会静默不再生效，
 * 所有异常退回 Spring 默认 JSON —— 所以 {@code GlobalExceptionHandlerCoverageTest}
 * 会断言它确实在生效（见该测试的注释）。
 */
@ControllerAdvice(basePackages = "com.example.springboot.controller")
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
     * 数据库约束冲突（外键 / 唯一键）。
     *
     * <p>为什么需要单独处理：这类异常原先落到下面的兜底分支，变成一句「系统错误」，
     * 管理员既不知道发生了什么、也不知道该怎么做。最典型的场景是删排片时
     * {@code ordered_ibfk_1 ON DELETE RESTRICT} 拒绝删除（BL-02）——正确提示应当是
     * 「该场次已有订单」。
     *
     * <p>提示刻意<b>不透露具体约束名与表名</b>：约束名是内部实现细节，且直接回显
     * 数据库报错会暴露表结构。这里只给一句可操作的中文。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public Result<Void> dataIntegrityError(DataIntegrityViolationException e) {
        String detail = e.getMostSpecificCause().getMessage();
        logger.warn("数据约束冲突: {}", detail);

        // 外键拒绝删除：绝大多数是「删排片时该场次已有订单」。给可操作的话术，
        // 而不是让管理员对着「系统错误」猜。
        if (detail != null && detail.contains("foreign key")) {
            return Result.error(ErrorCode.BUSINESS_CONFLICT.code(),
                    "该记录仍被其他数据引用，无法删除；如涉及场次请将状态改为「停售」");
        }
        return Result.error(ErrorCode.BUSINESS_CONFLICT.code(),
                "数据违反唯一性或完整性约束，请检查后重试");
    }

    /**
     * 请求参数类型不匹配 —— 例如 {@code GET /films/abc}（id 声明为 Integer）、
     * {@code ?pageNum=abc}。
     *
     * <p>这类异常原先落到兜底分支变成 500「系统错误」，但它是**调用方传错**，
     * 属于 400。Spring 的默认消息是英文且带内部方法签名（
     * "Method parameter 'id': Failed to convert value of type 'java.lang.String'"），
     * 因此这里只回一句中文，具体参数名写进日志。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public Result<Void> typeMismatch(MethodArgumentTypeMismatchException e) {
        logger.warn("参数类型不匹配: name={}, value={}, requiredType={}",
                e.getName(), e.getValue(),
                e.getRequiredType() == null ? "?" : e.getRequiredType().getSimpleName());
        return Result.error(ErrorCode.PARAM_INVALID.code(),
                "参数「" + e.getName() + "」格式不正确");
    }

    /**
     * 缺少必填的查询参数 —— 例如 {@code GET /films/search}（未带 title）、
     * {@code GET /orders/seats}（未带 recordId）。
     *
     * <p>原先同样落到兜底变成 500。缺参数是调用方的问题，应回 400。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public Result<Void> missingParameter(MissingServletRequestParameterException e) {
        logger.warn("缺少必填参数: {}", e.getParameterName());
        return Result.error(ErrorCode.PARAM_INVALID.code(),
                "缺少必填参数「" + e.getParameterName() + "」");
    }

    /**
     * 缺少请求体或 JSON 无法解析 —— 例如空 body 的 POST、语法错误的 JSON。
     *
     * <p>原先落到兜底变成 500。请求体是调用方给的，格式不对属 400。
     * 不回显解析器的原始消息：它带完整的类名与方法签名，属于内部细节。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public Result<Void> unreadableBody(HttpMessageNotReadableException e) {
        logger.warn("请求体无法解析: {}", e.getMessage());
        return Result.error(ErrorCode.PARAM_INVALID.code(), "请求体缺失或格式不正确");
    }

    /** 不支持的请求方法（如对只读端点发 DELETE）—— 应为 405 语义，归到参数错误档 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public Result<Void> methodNotSupported(HttpRequestMethodNotSupportedException e) {
        logger.warn("不支持的请求方法: {}", e.getMessage());
        return Result.error(ErrorCode.PARAM_INVALID.code(),
                "该接口不支持 " + e.getMethod() + " 请求");
    }

    /** 不支持的 Content-Type（如给 JSON 接口发 text/plain） */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseBody
    public Result<Void> mediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        logger.warn("不支持的 Content-Type: {}", e.getContentType());
        return Result.error(ErrorCode.FILE_INVALID.code(),
                "不支持的请求格式，请使用 " + e.getSupportedMediaTypes());
    }

    /**
     * 处理所有未捕获的Exception异常
     *
     * <p>走到这里的都应是<strong>服务端自身</strong>的问题（空指针、SQL 写错、依赖故障）。
     * 前端拿到的「系统错误」本身不携带信息，排查只能靠日志 —— 所以这里用 error 级别
     * 并保留完整堆栈。若某类异常明明由调用方引起却落到这里，正确做法是给它加一个
     * 上面的具名处理器，而不是放宽这里。
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result<Void> error(Exception e) {
        logger.error("系统异常: type={}, message={}", e.getClass().getName(), e.getMessage(), e);
        return Result.error();
    }
}