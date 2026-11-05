package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "quyenhan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuyenHan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_quyen_han")
    private Long maQuyenHan;

    @Column(name = "ten_quyen_han", nullable = false, unique = true, length = 100)
    private String tenQuyenHan;

    @Column(name = "nhom_quyen", length = 100)
    private String nhomQuyen;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @CreationTimestamp
    @Column(name = "thoi_diem_tao", nullable = false, updatable = false)
    private LocalDateTime thoiDiemTao;

    @UpdateTimestamp
    @Column(name = "thoi_diem_cap_nhat", nullable = false)
    private LocalDateTime thoiDiemCapNhat;

    // Helper methods for compatibility
    public Long getPermissionId() {
        return maQuyenHan;
    }

    public void setPermissionId(Long permissionId) {
        this.maQuyenHan = permissionId;
    }

    public String getName() {
        return tenQuyenHan;
    }

    public void setName(String name) {
        this.tenQuyenHan = name;
    }

    public String getDescription() {
        return moTa;
    }

    public void setDescription(String description) {
        this.moTa = description;
    }
}
