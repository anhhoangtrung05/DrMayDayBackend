package com.drmayday.authrbac.security;

import io.jsonwebtoken.Claims;

public interface JwtTokenProvider {

    String generateToken(Integer userId, String phone, String roleName);

    Claims parseAndValidateClaims(String token);

    Integer getUserIdFromClaims(Claims claims);

    String getRoleFromClaims(Claims claims);

    String getPhoneFromClaims(Claims claims);
}
