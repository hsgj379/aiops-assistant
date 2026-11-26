package com.aiops.assistant.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 72, message = "密码长度需在 8~72 之间")
    private String password;

    /** 可选字段；留空时不写入数据库 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
