package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vaitro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_vai_tro")
    private Long roleId;

    @Column(name = "ten_vai_tro", nullable = false, length = 50)
    private String name;

    @Column(name = "ten_hien_thi", nullable = false, length = 100)
    private String displayName;

    @Column(name = "mo_ta_vai_tro")
    private String description;

    @Column(name = "ma_vai_tro_cha")
    private Long parentRoleId;

    @Column(name = "thoi_diem_tao", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "thoi_diem_cap_nhat", nullable = false)
    private OffsetDateTime updatedAt;

    public Role(Long roleId, String name, String displayName, String description,
                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(roleId, name, displayName, description, null, createdAt, updatedAt);
    }

    @PrePersist
    void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
