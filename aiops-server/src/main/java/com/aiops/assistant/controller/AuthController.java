package com.aiops.assistant.controller;

import com.aiops.assistant.common.Result;
import com.aiops.assistant.dto.AuthResponse;
import com.aiops.assistant.dto.LoginRequest;
import com.aiops.assistant.dto.RegisterRequest;
import com.aiops.assistant.dto.UserInfoResponse;
import com.aiops.assistant.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserInfoResponse> me(Authentication authentication) {
        return Result.ok(authService.currentUser(authentication.getName()));
    }
}
