package com.aiops.assistant.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 生成与解析（JJWT 0.11.5，HS256）。
 * 密钥只能来自环境变量 JWT_SECRET，未配置或强度不足时启动直接失败。
 */
@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "uid";

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${aiops.security.jwt.secret:}") String secret,
                   @Value("${aiops.security.jwt.expire-hours:24}") long expireHours) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "JWT_SECRET 未配置：请设置 Base64 编码的至少 32 字节（256-bit）随机密钥");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret.trim());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("JWT_SECRET 必须是合法的 Base64 字符串", ex);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET 解码后长度不足 32 字节（HS256 至少需要 256-bit 密钥）");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expireMillis = expireHours * 60L * 60L * 1000L;
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim(CLAIM_USER_ID, userId)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并校验签名与过期时间；Token 非法时抛出 JwtException / IllegalArgumentException。
     */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsername(Claims claims) {
        return claims.getSubject();
    }

    public Long getUserId(Claims claims) {
        Object value = claims.get(CLAIM_USER_ID);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
}
