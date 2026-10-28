package com.example.drmayday.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vaitro")
public class VaiTro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_vai_tro")
    private Long maVaiTro;

    @Column(name = "ten_vai_tro", nullable = false)
    private String tenVaiTro;

    @Column(name = "ten_hien_thi", nullable = false)
    private String tenHienThi;

    @Column(name = "mo_ta_vai_tro")
    private String moTaVaiTro;

    @Column(name = "thoi_diem_tao")
    private OffsetDateTime thoiDiemTao;

    @Column(name = "thoi_diem_cap_nhat")
    private OffsetDateTime thoiDiemCapNhat;

    public VaiTro() {
    }

    public VaiTro(Long maVaiTro, String tenVaiTro, String tenHienThi, String moTaVaiTro,
                  OffsetDateTime thoiDiemTao, OffsetDateTime thoiDiemCapNhat) {
        this.maVaiTro = maVaiTro;
        this.tenVaiTro = tenVaiTro;
        this.tenHienThi = tenHienThi;
        this.moTaVaiTro = moTaVaiTro;
        this.thoiDiemTao = thoiDiemTao != null ? thoiDiemTao : OffsetDateTime.now();
        this.thoiDiemCapNhat = thoiDiemCapNhat != null ? thoiDiemCapNhat : OffsetDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (thoiDiemTao == null) thoiDiemTao = OffsetDateTime.now();
        if (thoiDiemCapNhat == null) thoiDiemCapNhat = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        thoiDiemCapNhat = OffsetDateTime.now();
    }

    public Long getMaVaiTro() {
        return maVaiTro;
    }

    public void setMaVaiTro(Long maVaiTro) {
        this.maVaiTro = maVaiTro;
    }

    public String getTenVaiTro() {
        return tenVaiTro;
    }

    public void setTenVaiTro(String tenVaiTro) {
        this.tenVaiTro = tenVaiTro;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public void setTenHienThi(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getMoTaVaiTro() {
        return moTaVaiTro;
    }

    public void setMoTaVaiTro(String moTaVaiTro) {
        this.moTaVaiTro = moTaVaiTro;
    }

    public OffsetDateTime getThoiDiemTao() {
        return thoiDiemTao;
    }

    public void setThoiDiemTao(OffsetDateTime thoiDiemTao) {
        this.thoiDiemTao = thoiDiemTao;
    }

    public OffsetDateTime getThoiDiemCapNhat() {
        return thoiDiemCapNhat;
    }

    public void setThoiDiemCapNhat(OffsetDateTime thoiDiemCapNhat) {
        this.thoiDiemCapNhat = thoiDiemCapNhat;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long maVaiTro;
        private String tenVaiTro;
        private String tenHienThi;
        private String moTaVaiTro;
        private OffsetDateTime thoiDiemTao;
        private OffsetDateTime thoiDiemCapNhat;

        public Builder maVaiTro(Long maVaiTro) {
            this.maVaiTro = maVaiTro;
            return this;
        }

        public Builder tenVaiTro(String tenVaiTro) {
            this.tenVaiTro = tenVaiTro;
            return this;
        }

        public Builder tenHienThi(String tenHienThi) {
            this.tenHienThi = tenHienThi;
            return this;
        }

        public Builder moTaVaiTro(String moTaVaiTro) {
            this.moTaVaiTro = moTaVaiTro;
            return this;
        }

        public Builder thoiDiemTao(OffsetDateTime thoiDiemTao) {
            this.thoiDiemTao = thoiDiemTao;
            return this;
        }

        public Builder thoiDiemCapNhat(OffsetDateTime thoiDiemCapNhat) {
            this.thoiDiemCapNhat = thoiDiemCapNhat;
            return this;
        }

        public VaiTro build() {
            return new VaiTro(maVaiTro, tenVaiTro, tenHienThi, moTaVaiTro, thoiDiemTao, thoiDiemCapNhat);
        }
    }
}
