package com.tsa.api.common;

import cn.dev33.satoken.exception.NotLoginException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：Controller 抛出的任何异常都在这里被"翻译"成统一 Result。
 *
 * <p>教学要点：有了它，业务代码里可以放心 throw，不用到处 try-catch 拼错误响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：可预期错误，返回对应业务码 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getResultCode(), e.getMessage());
    }

    /** 参数校验失败：@Valid 不通过时 Spring 抛此异常，把每个字段的错误拼成一句话 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", detail);
        return Result.fail(ResultCode.BAD_REQUEST, detail);
    }

    /** 请求体 JSON 解析失败（格式错误/非法编码）：属于客户端问题，返回 400 而非 500 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, "请求体 JSON 格式错误");
    }

    /** 未登录 / token 失效：Sa-Token 抛此异常 */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        return Result.fail(ResultCode.UNAUTHORIZED);
    }

    /** 404：接口路径不存在 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /** 兜底：任何没预料到的异常都记完整日志，但对外只返回模糊信息（不泄露堆栈） */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(HttpServletRequest request, Exception e) {
        log.error("未处理异常, uri={}", request.getRequestURI(), e);
        return Result.fail(ResultCode.ERROR);
    }
}
