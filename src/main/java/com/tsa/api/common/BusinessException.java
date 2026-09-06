package com.tsa.api.common;

import lombok.Getter;

/**
 * 业务异常：Service 层遇到"可预期"的错误时抛出（如重复注册、数据不存在）。
 *
 * <p>由 {@link GlobalExceptionHandler} 统一捕获并转换成 Result 返回，
 * Service 里不需要手动拼装错误响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    /** 需要更具体的提示时用这个构造器，例如：throw new BusinessException(DATA_NOT_FOUND, "成员 id=3 不存在") */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
