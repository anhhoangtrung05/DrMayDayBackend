package com.drmayday.authrbac.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;

    @Column(name = "ngay_sinh")
    private LocalDate dateOfBirth;

    @Column(name = "nghe_nghiep", length = 100)
    private String occupation;

    @Column(name = "dan_toc", length = 100)
    private String ethnic;

    @Column(name = "dia_chi_so_nha_thon_pho", length = 255)
    private String address;

    @Column(name = "dia_chi_xa_phuong", length = 100)
    private String ward;

    @Column(name = "dia_chi_tinh_tp", length = 100)
    private String city;

    @Column(name = "noi_lam_viec", length = 100)
    private String workplace;

    @Column(name = "bhyt_so_the", length = 100)
    private String bhytCode;

    @Column(name = "bhyt_gia_tri_den")
    private LocalDate bhytExpiryDate;

    @Column(name = "nguoi_nha_ho_ten_dia_chi", length = 100)
    private String relativeNameAddress;

    @Column(name = "nguoi_nha_so_dien_thoai", length = 100)
    private String relativePhone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Patient() {
    }

    public Patient(Integer userId, User user, Integer roleId, Role role, LocalDate dateOfBirth,
                   String occupation, String ethnic, String address, String ward, String city,
                   String workplace, String bhytCode, LocalDate bhytExpiryDate,
                   String relativeNameAddress, String relativePhone) {
        this.userId = userId;
        this.user = user;
        this.roleId = roleId;
        this.role = role;
        this.dateOfBirth = dateOfBirth;
        this.occupation = occupation;
        this.ethnic = ethnic;
        this.address = address;
        this.ward = ward;
        this.city = city;
        this.workplace = workplace;
        this.bhytCode = bhytCode;
        this.bhytExpiryDate = bhytExpiryDate;
        this.relativeNameAddress = relativeNameAddress;
        this.relativePhone = relativePhone;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getEthnic() {
        return ethnic;
    }

    public void setEthnic(String ethnic) {
        this.ethnic = ethnic;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getWorkplace() {
        return workplace;
    }

    public void setWorkplace(String workplace) {
        this.workplace = workplace;
    }

    public String getBhytCode() {
        return bhytCode;
    }

    public void setBhytCode(String bhytCode) {
        this.bhytCode = bhytCode;
    }

    public LocalDate getBhytExpiryDate() {
        return bhytExpiryDate;
    }

    public void setBhytExpiryDate(LocalDate bhytExpiryDate) {
        this.bhytExpiryDate = bhytExpiryDate;
    }

    public String getRelativeNameAddress() {
        return relativeNameAddress;
    }

    public void setRelativeNameAddress(String relativeNameAddress) {
        this.relativeNameAddress = relativeNameAddress;
    }

    public String getRelativePhone() {
        return relativePhone;
    }

    public void setRelativePhone(String relativePhone) {
        this.relativePhone = relativePhone;
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

    public Integer getYearOfBirth() {
        return dateOfBirth != null ? dateOfBirth.getYear() : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer userId;
        private User user;
        private Integer roleId;
        private Role role;
        private LocalDate dateOfBirth;
        private String occupation;
        private String ethnic;
        private String address;
        private String ward;
        private String city;
        private String workplace;
        private String bhytCode;
        private LocalDate bhytExpiryDate;
        private String relativeNameAddress;
        private String relativePhone;

        public Builder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder roleId(Integer roleId) {
            this.roleId = roleId;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder occupation(String occupation) {
            this.occupation = occupation;
            return this;
        }

        public Builder ethnic(String ethnic) {
            this.ethnic = ethnic;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder ward(String ward) {
            this.ward = ward;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder workplace(String workplace) {
            this.workplace = workplace;
            return this;
        }

        public Builder bhytCode(String bhytCode) {
            this.bhytCode = bhytCode;
            return this;
        }

        public Builder bhytExpiryDate(LocalDate bhytExpiryDate) {
            this.bhytExpiryDate = bhytExpiryDate;
            return this;
        }

        public Builder relativeNameAddress(String relativeNameAddress) {
            this.relativeNameAddress = relativeNameAddress;
            return this;
        }

        public Builder relativePhone(String relativePhone) {
            this.relativePhone = relativePhone;
            return this;
        }

        public Patient build() {
            return new Patient(userId, user, roleId, role, dateOfBirth, occupation, ethnic, address,
                    ward, city, workplace, bhytCode, bhytExpiryDate, relativeNameAddress, relativePhone);
        }
    }
}
