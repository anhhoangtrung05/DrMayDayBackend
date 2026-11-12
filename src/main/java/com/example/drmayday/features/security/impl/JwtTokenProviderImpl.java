package com.example.drmayday.features.security.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.features.security.JwtTokenProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtTokenProviderImpl implements JwtTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProviderImpl.class);

    // Redis key prefixes
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";
    private static final String USER_VERSION_KEY_PREFIX = "jwt:user-version:";

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final StringRedisTemplate redisTemplate;

    public JwtTokenProviderImpl(
            @Value("${jwt.secret:default-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-algorithm-safety}") String secret,
            @Value("${jwt.access-token-expiration-ms:3600000}") long accessTokenExpirationMs,
            StringRedisTemplate redisTemplate) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            this.key = Keys.hmacShaKeyFor(padded);
        } else {
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String generateToken(Long userId, String phone, String roleName) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(Objects.requireNonNull(userId, "userId").toString())
                .claim("phone", phone)
                .claim("role", roleName)
                .claim("tokenVersion", getTokenVersion(userId)) // Read the current version from Redis
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    @Override
    public void invalidateToken(String token) {
        Claims claims = parseAndValidateClaims(token);
        String jti = claims.getId();
        Date expiration = claims.getExpiration();
        Long userId = getUserId(claims);
        if (jti == null || jti.isBlank() || expiration == null || userId == null) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        blacklistToken(jti, expiration);
    }

    @Override
    public void invalidateUserTokens(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.USER_NOT_AUTHENTICATED);
        }

        try {
            // Atomically increment the version in Redis, initializing it to 1 if it does not exist.
            redisTemplate.opsForValue().increment(USER_VERSION_KEY_PREFIX + userId);
        } catch (Exception ex) {
            log.error("Unable to update the token version in Redis for userId={}", userId, ex);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Claims parseAndValidateClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        String cleanToken = token.trim();
        if (cleanToken.regionMatches(true, 0, "Bearer ", 0, 7)) {
            cleanToken = cleanToken.substring(7).trim();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(cleanToken)
                    .getPayload();

            validateTokenVersion(claims);

            String jti = claims.getId();
            if (jti != null && isTokenBlacklisted(jti)) {
                log.warn("Token with jti={} is on the Redis blacklist", jti);
                throw new AppException(ErrorCode.TOKEN_INVALID);
            }

            return claims;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token has expired: {}", ex.getMessage());
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        } catch (AppException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT token is invalid (signature or structure): {}", ex.getMessage());
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    private boolean isTokenBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
        } catch (Exception ex) {
            log.error("Unable to check the token blacklist in Redis", ex);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateTokenVersion(Claims claims) {
        Long userId = getUserId(claims);
        if (userId == null) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        Object versionClaim = claims.get("tokenVersion");
        long tokenVersion = versionClaim instanceof Number number ? number.longValue() : 0L;

        if (tokenVersion < getTokenVersion(userId)) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    private long getTokenVersion(Long userId) {
        if (userId == null) {
            return 0L;
        }
        try {
            String versionStr = redisTemplate.opsForValue().get(USER_VERSION_KEY_PREFIX + userId);
            return versionStr != null ? Long.parseLong(versionStr) : 0L;
        } catch (Exception ex) {
            log.error("Unable to retrieve the token version from Redis for userId={}", userId, ex);
            return 0L; // Safely fall back to allowing the token when Redis is unavailable; this can be changed to fail closed.
        }
    }

    private void blacklistToken(String jti, Date expiration) {
        Duration ttl = Duration.between(Instant.now(), expiration.toInstant());
        if (ttl.isPositive()) {
            try {
                redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, "1", ttl);
            } catch (Exception ex) {
                log.error("Unable to add the token to the Redis blacklist", ex);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }
    }

    private Long getUserId(Claims claims) {
        if (claims == null) {
            return null;
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
