package com.aiops.assistant.service;

import com.aiops.assistant.common.ConflictException;
import com.aiops.assistant.config.JwtUtil;
import com.aiops.assistant.dto.AuthResponse;
import com.aiops.assistant.dto.LoginRequest;
import com.aiops.assistant.dto.RegisterRequest;
import com.aiops.assistant.dto.UserInfoResponse;
import com.aiops.assistant.entity.User;
import com.aiops.assistant.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public UserInfoResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail().trim());
        }
        // role / status 与数据库默认值一致，不赋予任何管理员权限
        user.setRole("USER");
        user.setStatus((byte) 1);

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // 并发注册时 uk_username 唯一索引兜底
            throw new ConflictException("用户名已存在");
        }
        return UserInfoResponse.from(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));

        if (!user.isEnabled()) {
            throw new DisabledException("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getId());
    }

    @Transactional(readOnly = true)
    public UserInfoResponse currentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return UserInfoResponse.from(user);
    }
}
