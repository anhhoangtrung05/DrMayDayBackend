package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "taikhoan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tai_khoan")
    private Long accountId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ma_nguoi", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ma_vai_tro", nullable = false)
    private Role role;

    @Column(name = "ten_dang_nhap", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "mat_khau_ma_hoa", nullable = false)
    private String passwordHash;

    @Column(name = "trang_thai_hoat_dong", nullable = false)
    @Builder.Default
    private Boolean active = true;

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
