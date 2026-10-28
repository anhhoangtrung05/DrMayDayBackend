package com.example.drmayday.features.authorization.security;

import io.jsonwebtoken.Claims;

public interface JwtTokenProvider {

    String generateToken(Long userId, String phone, String roleName);

    default String generateToken(Integer userId, String phone, String roleName) {
        return generateToken(userId != null ? userId.longValue() : null, phone, roleName);
    }

    Claims parseAndValidateClaims(String token);

    Long getUserIdFromClaims(Claims claims);

    String getRoleFromClaims(Claims claims);

    String getPhoneFromClaims(Claims claims);
}
