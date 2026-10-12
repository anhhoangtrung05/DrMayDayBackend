package com.drmayday.authrbac.security.impl;

import com.drmayday.authrbac.repository.TokenBlacklistRepository;
import com.drmayday.authrbac.security.JwtTokenProvider;
import com.drmayday.common.exception.AppException;
import com.drmayday.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProviderImpl implements JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProviderImpl.class);

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    public JwtTokenProviderImpl(
            @Value("${jwt.secret:default-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-algorithm-safety}") String secret,
            @Value("${jwt.access-token-expiration-ms:3600000}") long accessTokenExpirationMs,
            TokenBlacklistRepository tokenBlacklistRepository) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            this.key = Keys.hmacShaKeyFor(padded);
        } else {
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    @Override
    public String generateToken(Integer userId, String phone, String roleName) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(phone)
                .claim("userId", userId)
                .claim("role", roleName)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    @Override
    public Claims parseAndValidateClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            if (jti != null && tokenBlacklistRepository.existsByJti(jti)) {
                log.warn("Token với jti={} nằm trong danh sách token_blacklist", jti);
                throw new AppException(ErrorCode.TOKEN_INVALID);
            }

            return claims;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token đã hết hạn: {}", ex.getMessage());
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT token không hợp lệ: {}", ex.getMessage());
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    @Override
    public Integer getUserIdFromClaims(Claims claims) {
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Number) {
            return ((Number) userIdObj).intValue();
        }
        return null;
    }

    @Override
    public String getRoleFromClaims(Claims claims) {
        return (String) claims.get("role");
    }

    @Override
    public String getPhoneFromClaims(Claims claims) {
        return claims.getSubject();
    }
}
