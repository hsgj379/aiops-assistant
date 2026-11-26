package com.aiops.assistant.dto;

/**
 * 登录成功响应：只暴露 Token 与最小用户标识，不含密码哈希。
 */
public class AuthResponse {

    private String token;
    private String username;
    private Long userId;

    public AuthResponse() {
    }

    public AuthResponse(String token, String username, Long userId) {
        this.token = token;
        this.username = username;
        this.userId = userId;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
