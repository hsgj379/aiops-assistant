package com.aiops.assistant.common;

/**
 * AI 模块统一异常：携带对外的 HTTP 状态码与安全提示文案。
 * 不保存上游原始响应、请求头或密钥，避免泄露。
 */
public class AiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public AiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
