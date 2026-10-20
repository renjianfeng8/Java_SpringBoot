package com.example.springboot;

import com.example.springboot.common.Result;
import com.example.springboot.exception.CustomException;
import com.example.springboot.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异常映射守卫：<b>调用方引起的错误不得变成「系统错误」</b>。
 *
 * <p>背景（2026-10-18 实测）：`GlobalExceptionHandler` 除业务异常外只有一个兜底分支，
 * 下列请求全部返回 500「系统错误」——实测共 11 条路径：
 *
 * <pre>
 * GET  /films/abc                      路径参数类型错
 * GET  /films/search                   缺必填查询参数 title
 * GET  /films/by-cinema                缺 cinemaId
 * GET  /orders/seats                   缺 recordId
 * GET  /marks/by-film                  缺 filmId
 * GET  /films/page?pageNum=abc         查询参数类型错
 * POST /films        空 body / {}      请求体缺失 / 空对象
 * PUT  /orders/1/pay 空 body
 * POST /auth/register 空 body
 * </pre>
 *
 * <p>三次工单（BL-01/02/03）都撞在同一个薄弱点上：只分类处理了业务异常，
 * Spring MVC 自身的参数类异常一律漏进兜底。本类把每一类都钉住。
 */
class ExceptionMappingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** 路径/查询参数类型错（如 id=abc）→ 400，不是 500 */
    @Test
    void typeMismatchBecomesParamInvalid() {
        MethodArgumentTypeMismatchException e = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "id", null, new NumberFormatException("For input string: \"abc\""));

        Result<Void> r = handler.typeMismatch(e);

        assertThat(r.getCode()).isEqualTo("400");
        assertThat(r.getMsg()).contains("id").contains("格式不正确");
    }

    /** 缺必填查询参数 → 400 */
    @Test
    void missingParameterBecomesParamInvalid() {
        MissingServletRequestParameterException e =
                new MissingServletRequestParameterException("recordId", "Integer");

        Result<Void> r = handler.missingParameter(e);

        assertThat(r.getCode()).isEqualTo("400");
        assertThat(r.getMsg()).contains("recordId");
    }

    /** 请求体缺失或 JSON 语法错 → 400，且不回显解析器的内部消息 */
    @Test
    void unreadableBodyBecomesParamInvalid() {
        // 单参构造器在 Spring 6.2 已弃用（Sonar java:S1874）；处理器只读 message，给一个空输入即可
        HttpMessageNotReadableException e =
                new HttpMessageNotReadableException("Required request body is missing: "
                        + "public com.example.springboot.common.Result<java.lang.Void> com.example.springboot.common.BaseController.add(R)",
                        new MockHttpInputMessage(new byte[0]));

        Result<Void> r = handler.unreadableBody(e);

        assertThat(r.getCode()).isEqualTo("400");
        assertThat(r.getMsg()).isEqualTo("请求体缺失或格式不正确");
    }

    /** 不支持的请求方法 → 参数错误档，而不是 500 */
    @Test
    void methodNotSupportedIsNotSystemError() {
        HttpRequestMethodNotSupportedException e =
                new HttpRequestMethodNotSupportedException("PATCH");

        Result<Void> r = handler.methodNotSupported(e);

        assertThat(r.getCode()).isNotEqualTo("500");
        assertThat(r.getMsg()).contains("PATCH");
    }

    /** 不支持的 Content-Type → 415 语义 */
    @Test
    void mediaTypeNotSupportedIsFileInvalid() {
        HttpMediaTypeNotSupportedException e =
                new HttpMediaTypeNotSupportedException("text/plain");

        Result<Void> r = handler.mediaTypeNotSupported(e);

        assertThat(r.getCode()).isEqualTo("415");
    }

    /** 真正的服务端异常仍走兜底，且返回「系统错误」——这条不能被上面的改动放宽 */
    @Test
    void genuineServerErrorStillFallsBackToSystemError() {
        Result<Void> r = handler.error(new NullPointerException("boom"));

        assertThat(r.getCode()).isEqualTo("500");
        assertThat(r.getMsg()).isEqualTo("系统错误");
    }

    /**
     * 死锁牺牲者 → 409「可重试」，不是 500。
     *
     * <p>实测来源：把 BL-01 的影厅行锁临时去掉，两个并发建排片请求撞出死锁，
     * 原先一路落到兜底变成「系统错误」。这里用父类型
     * {@link PessimisticLockingFailureException} 构造，避免引用已弃用的
     * {@code DeadlockLoserDataAccessException}（BL-52）。
     */
    @Test
    void deadlockBecomesRetryableConflict() {
        Result<Void> r = handler.concurrentConflict(
                new PessimisticLockingFailureException(
                        "Deadlock found when trying to get lock", new RuntimeException()));

        assertThat(r.getCode()).isEqualTo("409");
        assertThat(r.getMsg()).contains("稍后重试");
    }

    /** 等锁超时与死锁同一档处理：它同样是 {@link PessimisticLockingFailureException} 的子类 */
    @Test
    void lockWaitTimeoutBecomesRetryableConflict() {
        Result<Void> r = handler.concurrentConflict(
                new org.springframework.dao.CannotAcquireLockException(
                        "Lock wait timeout exceeded", new RuntimeException()));

        assertThat(r.getCode()).isEqualTo("409");
        assertThat(r.getMsg()).contains("重试");
    }

    /** 业务异常仍原样透传自己的 code/msg */
    @Test
    void customExceptionKeepsItsOwnCode() {
        Result<Void> r = handler.error(new CustomException(com.example.springboot.common.enums.ErrorCode.NOT_FOUND, "排片不存在"));

        assertThat(r.getCode()).isEqualTo("404");
        assertThat(r.getMsg()).isEqualTo("排片不存在");
    }
}
