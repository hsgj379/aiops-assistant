package com.aiops.assistant.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

/**
 * 统一把认证相关异常映射为项目通用 Result 结构，且不向客户端暴露内部堆栈。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null
                ? "参数校验失败"
                : fieldError.getField() + " " + fieldError.getDefaultMessage();
        return Result.fail(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        return Result.fail(400, "参数校验失败");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return Result.fail(400, "请求体格式错误");
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleConflict(ConflictException ex) {
        return Result.fail(409, ex.getMessage());
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleDisabled(DisabledException ex) {
        return Result.fail(401, "账号已被禁用");
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleBadCredentials(RuntimeException ex) {
        return Result.fail(401, "用户名或密码错误");
    }

    @ExceptionHandler(AiException.class)
    public ResponseEntity<Result<Void>> handleAi(AiException ex) {
        int status = ex.getStatus();
        if (status >= 500) {
            log.warn("AI 服务异常: status={}, message={}", status, ex.getMessage());
        }
        return ResponseEntity.status(status).body(Result.fail(status, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnexpected(Exception ex) {
        // 仅服务端记录（不含请求体 / 密码 / Token），客户端只看到通用提示
        log.error("未处理的异常: {}", ex.getClass().getName(), ex);
        return Result.fail(500, "服务器内部错误");
    }
}
