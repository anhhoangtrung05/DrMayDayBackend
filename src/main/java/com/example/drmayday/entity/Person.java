package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "nguoi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nguoi")
    private Long personId;

    @Column(name = "ho_ten", nullable = false, length = 150)
    private String fullName;

    @Column(name = "ngay_sinh")
    private LocalDate birthDate;

    @Column(name = "gioi_tinh", length = 3)
    private String gender;

    @Column(name = "so_dien_thoai", nullable = false, length = 10)
    private String phoneNumber;

    @Column(name = "dia_chi_tinh_tp", length = 100)
    private String provinceCity;

    @Column(name = "dia_chi_xa_phuong", length = 100)
    private String communeWard;

    @Column(name = "dia_chi_so_nha_thon_pho", length = 255)
    private String streetAddress;

    @Column(name = "thoi_diem_tao", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "thoi_diem_cap_nhat", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null || updatedAt.isBefore(createdAt)) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
