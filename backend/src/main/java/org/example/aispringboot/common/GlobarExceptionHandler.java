package org.example.aispringboot.common;

import lombok.extern.slf4j.Slf4j;
import org.example.aispringboot.exception.BusinessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一捕获 Controller 层抛出的异常，并将它们转换为统一的 Result 结构返回给前端，
 * 避免将异常堆栈直接暴露给客户端。
 */
@Slf4j
@RestControllerAdvice
public class GlobarExceptionHandler {

    // 处理参数校验异常（@Valid / @Validated）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handlerException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        // 把具体校验信息放到 msg，便于前端直接展示
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message, message);
    }

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage(), e.getData());
    }

    // 处理权限不足异常（@PreAuthorize 校验不通过）
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDenied(AccessDeniedException e) {
        log.warn("访问权限不足: {}", e.getMessage());
        return Result.error(ResultCode.AUTHORIZED_ERROR.getCode(), ResultCode.AUTHORIZED_ERROR.getMsg(), null);
    }

    // 兜底处理未预期的异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMsg(), null);
    }
}
