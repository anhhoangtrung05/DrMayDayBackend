package com.example.drmayday.features.authorization.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO phản hồi thông tin người dùng theo đặc tả quản trị Admin (API 9, 11, 12).
 * Lưu ý đặc tả backend:
 * - user_id, role_id trả về kiểu String.
 * - is_active trả về kiểu String ("1" hoặc "0").
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("role_id")
    private String roleId;

    @JsonProperty("is_active")
    private String isActive;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public UserResponse() {
    }

    public UserResponse(String userId, String fullName, String phone, String roleId,
                        String isActive, String createdAt, String updatedAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.roleId = roleId;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private String fullName;
        private String phone;
        private String roleId;
        private String isActive;
        private String createdAt;
        private String updatedAt;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder roleId(String roleId) {
            this.roleId = roleId;
            return this;
        }

        public Builder isActive(String isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserResponse build() {
            return new UserResponse(userId, fullName, phone, roleId, isActive, createdAt, updatedAt);
        }
    }
}
