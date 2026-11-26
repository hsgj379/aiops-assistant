package com.aiops.assistant.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 从 Authorization: Bearer &lt;token&gt; 恢复认证信息。
 * 无 Token 时直接放行；Token 非法 / 过期 / 用户被禁用时只清空上下文，
 * 由 Spring Security 的 EntryPoint 统一返回 401，绝不把解析异常变成 500。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIX.length()).trim();
            if (!token.isEmpty()) {
                try {
                    Claims claims = jwtUtil.parse(token);
                    String username = claims.getSubject();
                    if (username != null) {
                        UserDetails user = userDetailsService.loadUserByUsername(username);
                        if (user.isEnabled()) {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } else {
                            SecurityContextHolder.clearContext();
                        }
                    }
                } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                    // 过期 / 签名错误 / 格式错误 / 用户已删除：按未认证处理
                    SecurityContextHolder.clearContext();
                    log.debug("JWT 校验失败: {} ({})", ex.getClass().getSimpleName(), ex.getMessage());
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
