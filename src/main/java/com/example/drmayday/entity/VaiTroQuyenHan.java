package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vaitroquyenhan")
@IdClass(VaiTroQuyenHanId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaiTroQuyenHan {

    @Id
    @Column(name = "ma_vai_tro")
    private Long maVaiTro;

    @Id
    @Column(name = "ma_quyen_han")
    private Long maQuyenHan;

    @CreationTimestamp
    @Column(name = "thoi_diem_gan", nullable = false, updatable = false)
    private LocalDateTime thoiDiemGan;

    @Column(name = "ma_nguoi_gan")
    private Long maNguoiGan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_vai_tro", insertable = false, updatable = false)
    private VaiTro vaiTro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_quyen_han", insertable = false, updatable = false)
    private QuyenHan quyenHan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nguoi_gan", insertable = false, updatable = false)
    private Nguoi nguoiGan;

    // Compatibility methods
    public Long getRoleId() {
        return maVaiTro;
    }

    public void setRoleId(Long roleId) {
        this.maVaiTro = roleId;
    }

    public Long getPermissionId() {
        return maQuyenHan;
    }

    public void setPermissionId(Long permissionId) {
        this.maQuyenHan = permissionId;
    }
}
