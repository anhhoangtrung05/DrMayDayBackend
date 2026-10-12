package com.drmayday.authrbac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO yêu cầu chỉnh sửa thông tin người dùng từ trang quản trị Admin (API 12 edit_user).
 */
public class EditUserRequest {

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("role_id")
    private String roleId;

    @JsonProperty("is_active")
    private String isActive;

    public EditUserRequest() {
    }

    public EditUserRequest(String fullName, String roleId, String isActive) {
        this.fullName = fullName;
        this.roleId = roleId;
        this.isActive = isActive;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String fullName;
        private String roleId;
        private String isActive;

        public Builder fullName(String fullName) {
            this.fullName = fullName;
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

        public EditUserRequest build() {
            return new EditUserRequest(fullName, roleId, isActive);
        }
    }
}
