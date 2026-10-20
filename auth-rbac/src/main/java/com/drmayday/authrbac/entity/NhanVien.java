package com.drmayday.authrbac.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "nhanvien")
public class NhanVien implements Persistable<Long> {

    @Id
    @Column(name = "ma_nhan_vien")
    private Long maNhanVien;

    @Column(name = "ma_chi_nhanh", nullable = false)
    private Long maChiNhanh;

    @Transient
    private boolean isNew = true;

    public NhanVien() {
    }

    public NhanVien(Long maNhanVien, Long maChiNhanh) {
        this.maNhanVien = maNhanVien;
        this.maChiNhanh = maChiNhanh;
    }

    @Override
    public Long getId() {
        return maNhanVien;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    public void markNotNew() {
        this.isNew = false;
    }

    public Long getMaNhanVien() {
        return maNhanVien;
    }

    public void setMaNhanVien(Long maNhanVien) {
        this.maNhanVien = maNhanVien;
    }

    public Long getMaChiNhanh() {
        return maChiNhanh;
    }

    public void setMaChiNhanh(Long maChiNhanh) {
        this.maChiNhanh = maChiNhanh;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long maNhanVien;
        private Long maChiNhanh;

        public Builder maNhanVien(Long maNhanVien) {
            this.maNhanVien = maNhanVien;
            return this;
        }

        public Builder nguoi(Nguoi nguoi) {
            if (nguoi != null) {
                this.maNhanVien = nguoi.getMaNguoi();
            }
            return this;
        }

        public Builder maChiNhanh(Long maChiNhanh) {
            this.maChiNhanh = maChiNhanh;
            return this;
        }

        public NhanVien build() {
            return new NhanVien(maNhanVien, maChiNhanh);
        }
    }
}
