package com.tsa.api.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /**
     * Content-Type 缺失/不匹配（如 form-urlencoded 打 JSON 接口）：与上面同属客户端问题，
     * 与 B5 拆伪装层同理 —— 不接的话落 catch-all 伪装成 500，排查方向整个跑偏。
     */
    @ExceptionHandler(HttpMediaTypeException.class)
    public Result<Void> handleMediaType(HttpMediaTypeException e) {
        log.warn("媒体类型不支持: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, "Content-Type 不支持");
    }

    /**
     * 上传 body 击穿 spring.servlet.multipart 容器阈值（契约 C8）：
     * 与 FileController 的业务级 2MB 校验同一句提示——「超限」无论拦在哪一层，
     * 对客户端必须是同一个错误，否则前端要为两个阈值摆两套文案。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传大小击穿容器阈值: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, "文件超限或类型不支持");
    }

    /** 未登录 / token 失效：Sa-Token 抛此异常 */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        return Result.fail(ResultCode.UNAUTHORIZED);
    }

    /**
     * 已登录但角色不符：{@code StpUtil.checkRole("admin")} 对 /tsa/admin/** 挡非管理员会话时抛此异常。
     * 不接的话会落下方 catch-all 伪装成 500 —— 与 /tsa/admin/** 改判 checkRole（v1.3 防 openid/assoc
     * 冒充）配套：匿名 → NotLoginException(401)、登录但非 admin → 本码(403)，两种态严格分离。
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        return Result.fail(ResultCode.FORBIDDEN);
    }

    /** 404：接口路径不存在 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /**
     * 404：Boot 3.2+ 起，未映射路径实际先落静态资源处理器、miss 后抛 NoResourceFoundException
     * （不再走 NoHandlerFoundException）。上面那条不接它，就会被下方 catch-all 伪装成
     * HTTP 200 + code 500「服务器繁忙」——未实现的路由看着像服务器故障，排查全被带偏
     * （「联调三大坑」之三）。拆掉这层伪装：路径 miss 就老实回 404。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /** 兜底：任何没预料到的异常都记完整日志，但对外只返回模糊信息（不泄露堆栈） */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(HttpServletRequest request, Exception e) {
        log.error("未处理异常, uri={}", request.getRequestURI(), e);
        return Result.fail(ResultCode.ERROR);
    }
}
