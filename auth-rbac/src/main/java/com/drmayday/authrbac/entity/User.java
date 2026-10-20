package com.drmayday.authrbac.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "ho_ten", nullable = false, length = 255)
    private String fullName;

    @Column(name = "so_dien_thoai", nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "gioi_tinh", length = 10)
    private String gender;

    @Column(name = "reset_token", length = 255)
    private String resetToken;

    @Column(name = "reset_token_expires")
    private LocalDateTime resetTokenExpires;

    @Column(name = "is_active", nullable = false)
    private Integer isActive = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Patient patient;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Staff staff;

    public User() {
    }

    public User(Integer userId, String passwordHash, String fullName, String phone, String gender,
                String resetToken, LocalDateTime resetTokenExpires, Integer isActive,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.gender = gender;
        this.resetToken = resetToken;
        this.resetTokenExpires = resetTokenExpires;
        this.isActive = isActive != null ? isActive : 1;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getResetTokenExpires() {
        return resetTokenExpires;
    }

    public void setResetTokenExpires(LocalDateTime resetTokenExpires) {
        this.resetTokenExpires = resetTokenExpires;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
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

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
        if (patient != null) {
            patient.setUser(this);
        }
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
        if (staff != null) {
            staff.setUser(this);
        }
    }

    /**
     * Lấy ID vai trò thực tế của người dùng từ bảng con staffs hoặc patients.
     */
    public Integer getEffectiveRoleId() {
        if (staff != null && staff.getRoleId() != null) {
            return staff.getRoleId();
        }
        if (patient != null && patient.getRoleId() != null) {
            return patient.getRoleId();
        }
        return null;
    }

    /**
     * Lấy thực thể Role liên kết của người dùng.
     */
    public Role getEffectiveRole() {
        if (staff != null && staff.getRole() != null) {
            return staff.getRole();
        }
        if (patient != null && patient.getRole() != null) {
            return patient.getRole();
        }
        return null;
    }

    /**
     * Lấy tên vai trò (ví dụ: "admin", "patient", "doctor").
     */
    public String getEffectiveRoleName() {
        Role role = getEffectiveRole();
        return role != null ? role.getName() : "patient";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer userId;
        private String passwordHash;
        private String fullName;
        private String phone;
        private String gender;
        private String resetToken;
        private LocalDateTime resetTokenExpires;
        private Integer isActive = 1;

        public Builder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
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

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder resetToken(String resetToken) {
            this.resetToken = resetToken;
            return this;
        }

        public Builder resetTokenExpires(LocalDateTime resetTokenExpires) {
            this.resetTokenExpires = resetTokenExpires;
            return this;
        }

        public Builder isActive(Integer isActive) {
            this.isActive = isActive;
            return this;
        }

        public User build() {
            return new User(userId, passwordHash, fullName, phone, gender, resetToken, resetTokenExpires, isActive, null, null);
        }
    }
}
