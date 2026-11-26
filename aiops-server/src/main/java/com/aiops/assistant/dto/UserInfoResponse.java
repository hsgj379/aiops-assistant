package com.aiops.assistant.dto;

import com.aiops.assistant.entity.User;

import java.time.LocalDateTime;

/**
 * 用户信息响应，显式白名单字段，绝不包含 password_hash。
 */
public class UserInfoResponse {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String role;
    private Byte status;
    private String avatar;
    private LocalDateTime lastLoginAt;

    public static UserInfoResponse from(User user) {
        UserInfoResponse resp = new UserInfoResponse();
        resp.id = user.getId();
        resp.username = user.getUsername();
        resp.email = user.getEmail();
        resp.phone = user.getPhone();
        resp.role = user.getRole();
        resp.status = user.getStatus();
        resp.avatar = user.getAvatar();
        resp.lastLoginAt = user.getLastLoginAt();
        return resp;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public Byte getStatus() { return status; }
    public String getAvatar() { return avatar; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
}
