package com.example.drmayday.features.security.impl;

import com.example.drmayday.repository.TokenBlacklistRepository;
import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.features.security.JwtTokenProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtTokenProviderImpl implements JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProviderImpl.class);

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Set<String> inMemoryBlacklist = ConcurrentHashMap.newKeySet();
    private volatile boolean blacklistTableExists = false;

    public JwtTokenProviderImpl(
            @Value("${jwt.secret:default-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-algorithm-safety}") String secret,
            @Value("${jwt.access-token-expiration-ms:3600000}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs,
            @Autowired(required = false) TokenBlacklistRepository tokenBlacklistRepository,
            @Autowired(required = false) JdbcTemplate jdbcTemplate) {
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
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            if (jdbcTemplate != null) {
                Boolean exists = jdbcTemplate.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'token_blacklist')",
                        Boolean.class
                );
                this.blacklistTableExists = Boolean.TRUE.equals(exists);
                if (this.blacklistTableExists) {
                    log.info("Bảng token_blacklist đã sẵn sàng trong CSDL.");
                } else {
                    log.info("Bảng token_blacklist chưa có trong CSDL. Hệ thống tự động kích hoạt in-memory blacklist.");
                }
            }
        } catch (Exception ex) {
            this.blacklistTableExists = false;
            log.info("Không thể kiểm tra bảng token_blacklist qua information_schema, dùng in-memory blacklist.");
        }
    }

    @Override
    public String generateToken(Long userId, String phone, String roleName) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(phone)
                .claim("userId", userId)
                .claim("user_id", userId)
                .claim("phone", phone)
                .claim("role", roleName)
                .claim("role_name", roleName)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
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

            String jti = claims.getId();
            if (jti != null && isTokenBlacklisted(jti)) {
                log.warn("Token với jti={} nằm trong danh sách token_blacklist", jti);
                throw new AppException(ErrorCode.TOKEN_INVALID);
            }

            return claims;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token đã hết hạn: {}", ex.getMessage());
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        } catch (AppException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT token không hợp lệ (sai signature / cấu trúc): {}", ex.getMessage());
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    private boolean isTokenBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        if (inMemoryBlacklist.contains(jti)) {
            return true;
        }
        if (!blacklistTableExists || tokenBlacklistRepository == null) {
            return false;
        }
        try {
            return tokenBlacklistRepository.existsByJti(jti);
        } catch (Exception ex) {
            log.debug("Lỗi truy vấn token_blacklist CSDL, chuyển sang in-memory: {}", ex.getMessage());
            this.blacklistTableExists = false;
            return false;
        }
    }

    public void blacklistToken(String jti) {
        if (jti != null) {
            inMemoryBlacklist.add(jti);
        }
    }

    @Override
    public Long getUserIdFromClaims(Claims claims) {
        if (claims == null) {
            return null;
        }

        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            userIdObj = claims.get("user_id");
        }
        if (userIdObj == null) {
            userIdObj = claims.get("id");
        }

        if (userIdObj instanceof Number) {
            return ((Number) userIdObj).longValue();
        }
        if (userIdObj instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        // Fallback: Nếu subject là chuỗi số ID người dùng
        String sub = claims.getSubject();
        if (sub != null && sub.matches("^[0-9]+$") && sub.length() < 10) {
            try {
                return Long.parseLong(sub);
            } catch (NumberFormatException ignored) {
            }
        }

        return null;
    }

    @Override
    public String getRoleFromClaims(Claims claims) {
        if (claims == null) {
            return null;
        }

        Object roleObj = claims.get("role");
        if (roleObj == null) {
            roleObj = claims.get("roles");
        }
        if (roleObj == null) {
            roleObj = claims.get("role_name");
        }
        if (roleObj == null) {
            roleObj = claims.get("authorities");
        }

        if (roleObj instanceof String) {
            return (String) roleObj;
        }
        if (roleObj instanceof Collection<?> list && !list.isEmpty()) {
            return String.valueOf(list.iterator().next());
        }

        return null;
    }

    @Override
    public String getPhoneFromClaims(Claims claims) {
        if (claims == null) {
            return null;
        }

        String sub = claims.getSubject();
        if (sub != null && sub.matches("^[0-9]{10}$")) {
            return sub;
        }

        Object phoneObj = claims.get("phone");
        if (phoneObj == null) {
            phoneObj = claims.get("so_dien_thoai");
        }
        if (phoneObj != null) {
            return String.valueOf(phoneObj);
        }

        return sub;
    }
}
