package com.example.drmayday.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_blacklist")
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invalid_token_id")
    private Integer invalidTokenId;

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TokenBlacklist() {
    }

    public TokenBlacklist(Integer invalidTokenId, String jti, Integer userId, LocalDateTime expiresAt) {
        this.invalidTokenId = invalidTokenId;
        this.jti = jti;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public Integer getInvalidTokenId() {
        return invalidTokenId;
    }

    public void setInvalidTokenId(Integer invalidTokenId) {
        this.invalidTokenId = invalidTokenId;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer invalidTokenId;
        private String jti;
        private Integer userId;
        private LocalDateTime expiresAt;

        public Builder invalidTokenId(Integer invalidTokenId) {
            this.invalidTokenId = invalidTokenId;
            return this;
        }

        public Builder jti(String jti) {
            this.jti = jti;
            return this;
        }

        public Builder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public TokenBlacklist build() {
            return new TokenBlacklist(invalidTokenId, jti, userId, expiresAt);
        }
    }
}
