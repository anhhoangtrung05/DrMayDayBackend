package com.example.drmayday.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "nguoi")
public class Nguoi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nguoi")
    private Long maNguoi;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "gioi_tinh")
    private String gioiTinh;

    @Column(name = "so_dien_thoai", nullable = false)
    private String soDienThoai;

    @Column(name = "dia_chi_tinh_tp")
    private String diaChiTinhTp;

    @Column(name = "dia_chi_xa_phuong")
    private String diaChiXaPhuong;

    @Column(name = "dia_chi_so_nha_thon_pho")
    private String diaChiSoNhaThonPho;

    @Column(name = "thoi_diem_tao", nullable = false)
    private OffsetDateTime thoiDiemTao;

    @Column(name = "thoi_diem_cap_nhat", nullable = false)
    private OffsetDateTime thoiDiemCapNhat;



    public Nguoi() {
    }

    public Nguoi(Long maNguoi, String hoTen, LocalDate ngaySinh, String gioiTinh, String soDienThoai,
                 String diaChiTinhTp, String diaChiXaPhuong, String diaChiSoNhaThonPho,
                 OffsetDateTime thoiDiemTao, OffsetDateTime thoiDiemCapNhat) {
        this.maNguoi = maNguoi;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.soDienThoai = soDienThoai;
        this.diaChiTinhTp = diaChiTinhTp;
        this.diaChiXaPhuong = diaChiXaPhuong;
        this.diaChiSoNhaThonPho = diaChiSoNhaThonPho;
        this.thoiDiemTao = thoiDiemTao != null ? thoiDiemTao : OffsetDateTime.now();
        this.thoiDiemCapNhat = thoiDiemCapNhat != null ? thoiDiemCapNhat : OffsetDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (thoiDiemTao == null || thoiDiemTao.isAfter(OffsetDateTime.now().minusSeconds(10))) {
            thoiDiemTao = OffsetDateTime.now().minusSeconds(10);
        }
        if (thoiDiemCapNhat == null || thoiDiemCapNhat.isBefore(thoiDiemTao)) {
            thoiDiemCapNhat = thoiDiemTao;
        }
    }

    @PreUpdate
    public void preUpdate() {
        thoiDiemCapNhat = OffsetDateTime.now();
        if (thoiDiemTao != null && thoiDiemCapNhat.isBefore(thoiDiemTao)) {
            thoiDiemCapNhat = thoiDiemTao;
        }
    }

    public Long getMaNguoi() {
        return maNguoi;
    }

    public void setMaNguoi(Long maNguoi) {
        this.maNguoi = maNguoi;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public String getGioiTinh() {
        return gioiTinh;
    }

    public void setGioiTinh(String gioiTinh) {
        this.gioiTinh = gioiTinh;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getDiaChiTinhTp() {
        return diaChiTinhTp;
    }

    public void setDiaChiTinhTp(String diaChiTinhTp) {
        this.diaChiTinhTp = diaChiTinhTp;
    }

    public String getDiaChiXaPhuong() {
        return diaChiXaPhuong;
    }

    public void setDiaChiXaPhuong(String diaChiXaPhuong) {
        this.diaChiXaPhuong = diaChiXaPhuong;
    }

    public String getDiaChiSoNhaThonPho() {
        return diaChiSoNhaThonPho;
    }

    public void setDiaChiSoNhaThonPho(String diaChiSoNhaThonPho) {
        this.diaChiSoNhaThonPho = diaChiSoNhaThonPho;
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
        private Long maNguoi;
        private String hoTen;
        private LocalDate ngaySinh;
        private String gioiTinh;
        private String soDienThoai;
        private String diaChiTinhTp;
        private String diaChiXaPhuong;
        private String diaChiSoNhaThonPho;
        private OffsetDateTime thoiDiemTao;
        private OffsetDateTime thoiDiemCapNhat;

        public Builder maNguoi(Long maNguoi) {
            this.maNguoi = maNguoi;
            return this;
        }

        public Builder hoTen(String hoTen) {
            this.hoTen = hoTen;
            return this;
        }

        public Builder ngaySinh(LocalDate ngaySinh) {
            this.ngaySinh = ngaySinh;
            return this;
        }

        public Builder gioiTinh(String gioiTinh) {
            this.gioiTinh = gioiTinh;
            return this;
        }

        public Builder soDienThoai(String soDienThoai) {
            this.soDienThoai = soDienThoai;
            return this;
        }

        public Builder diaChiTinhTp(String diaChiTinhTp) {
            this.diaChiTinhTp = diaChiTinhTp;
            return this;
        }

        public Builder diaChiXaPhuong(String diaChiXaPhuong) {
            this.diaChiXaPhuong = diaChiXaPhuong;
            return this;
        }

        public Builder diaChiSoNhaThonPho(String diaChiSoNhaThonPho) {
            this.diaChiSoNhaThonPho = diaChiSoNhaThonPho;
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

        public Nguoi build() {
            return new Nguoi(maNguoi, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChiTinhTp, diaChiXaPhuong, diaChiSoNhaThonPho, thoiDiemTao, thoiDiemCapNhat);
        }
    }
}
