package org.example.aispringboot.exception;

import lombok.Getter;
import org.example.aispringboot.common.ResultCode;

/**
 * 业务异常
 * 用于在业务层主动抛出可预期的错误，由全局异常处理器统一捕获并转换为 Result 返回。
 *
 * @author system
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final Object data;

    /**
     * 默认业务异常，错误码为 ResultCode.BUSINESS_ERROR
     */
    public BusinessException(String message) {
        this(ResultCode.BUSINESS_ERROR.getCode(), message, null);
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMsg(), null);
    }

    public BusinessException(String code, String message) {
        this(code, message, null);
    }

    public BusinessException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }
}
