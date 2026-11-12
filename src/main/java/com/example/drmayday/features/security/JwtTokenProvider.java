package com.example.drmayday.features.security;

import io.jsonwebtoken.Claims;

public interface JwtTokenProvider {

    String generateToken(Long userId, String phone, String roleName);

    void invalidateToken(String token);

    void invalidateUserTokens(Long userId);

    default String generateToken(Integer userId, String phone, String roleName) {
        return generateToken(userId != null ? userId.longValue() : null, phone, roleName);
    }

    Claims parseAndValidateClaims(String token);
}
