package com.drmayday.authrbac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO yêu cầu tạo người dùng mới từ trang quản trị Admin (API 11 create_user).
 */
public class CreateUserRequest {

    @NotBlank(message = "2001")
    @JsonProperty("full_name")
    private String fullName;

    @NotBlank(message = "2001")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "2004")
    @JsonProperty("phone")
    private String phone;

    @NotBlank(message = "2001")
    @JsonProperty("password")
    private String password;

    @NotBlank(message = "2001")
    @JsonProperty("role_id")
    private String roleId;

    @JsonProperty("is_active")
    private String isActive = "1";

    public CreateUserRequest() {
    }

    public CreateUserRequest(String fullName, String phone, String password, String roleId, String isActive) {
        this.fullName = fullName;
        this.phone = phone;
        this.password = password;
        this.roleId = roleId;
        this.isActive = isActive != null ? isActive : "1";
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
        private String phone;
        private String password;
        private String roleId;
        private String isActive = "1";

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
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

        public CreateUserRequest build() {
            return new CreateUserRequest(fullName, phone, password, roleId, isActive);
        }
    }
}
