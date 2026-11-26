package com.aiops.assistant.common;

/**
 * 业务冲突（如用户名重复），由全局异常处理器映射为 HTTP 409。
 */
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
