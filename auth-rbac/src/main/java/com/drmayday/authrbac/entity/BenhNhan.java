package com.drmayday.authrbac.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;

@Entity
@Table(name = "benhnhan")
public class BenhNhan implements Persistable<Long> {

    @Id
    @Column(name = "ma_benh_nhan")
    private Long maBenhNhan;

    @Column(name = "ma_his")
    private String maHis;

    @Column(name = "nghe_nghiep")
    private String ngheNghiep;

    @Column(name = "dan_toc")
    private String danToc;

    @Column(name = "bhyt_so_the")
    private String bhytSoThe;

    @Column(name = "bhyt_gia_tri_den")
    private LocalDate bhytGiaTriDen;

    @Column(name = "nguoi_nha_ho_ten_dia_chi")
    private String nguoiNhaHoTenDiaChi;

    @Column(name = "nguoi_nha_so_dien_thoai")
    private String nguoiNhaSoDienThoai;

    @Transient
    private boolean isNew = true;

    public BenhNhan() {
    }

    public BenhNhan(Long maBenhNhan, String maHis, String ngheNghiep, String danToc,
                    String bhytSoThe, LocalDate bhytGiaTriDen, String nguoiNhaHoTenDiaChi, String nguoiNhaSoDienThoai) {
        this.maBenhNhan = maBenhNhan;
        this.maHis = maHis;
        this.ngheNghiep = ngheNghiep;
        this.danToc = danToc;
        this.bhytSoThe = bhytSoThe;
        this.bhytGiaTriDen = bhytGiaTriDen;
        this.nguoiNhaHoTenDiaChi = nguoiNhaHoTenDiaChi;
        this.nguoiNhaSoDienThoai = nguoiNhaSoDienThoai;
    }

    @Override
    public Long getId() {
        return maBenhNhan;
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

    public Long getMaBenhNhan() {
        return maBenhNhan;
    }

    public void setMaBenhNhan(Long maBenhNhan) {
        this.maBenhNhan = maBenhNhan;
    }

    public String getMaHis() {
        return maHis;
    }

    public void setMaHis(String maHis) {
        this.maHis = maHis;
    }

    public String getNgheNghiep() {
        return ngheNghiep;
    }

    public void setNgheNghiep(String ngheNghiep) {
        this.ngheNghiep = ngheNghiep;
    }

    public String getDanToc() {
        return danToc;
    }

    public void setDanToc(String danToc) {
        this.danToc = danToc;
    }

    public String getBhytSoThe() {
        return bhytSoThe;
    }

    public void setBhytSoThe(String bhytSoThe) {
        this.bhytSoThe = bhytSoThe;
    }

    public LocalDate getBhytGiaTriDen() {
        return bhytGiaTriDen;
    }

    public void setBhytGiaTriDen(LocalDate bhytGiaTriDen) {
        this.bhytGiaTriDen = bhytGiaTriDen;
    }

    public String getNguoiNhaHoTenDiaChi() {
        return nguoiNhaHoTenDiaChi;
    }

    public void setNguoiNhaHoTenDiaChi(String nguoiNhaHoTenDiaChi) {
        this.nguoiNhaHoTenDiaChi = nguoiNhaHoTenDiaChi;
    }

    public String getNguoiNhaSoDienThoai() {
        return nguoiNhaSoDienThoai;
    }

    public void setNguoiNhaSoDienThoai(String nguoiNhaSoDienThoai) {
        this.nguoiNhaSoDienThoai = nguoiNhaSoDienThoai;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long maBenhNhan;
        private String maHis;
        private String ngheNghiep;
        private String danToc;
        private String bhytSoThe;
        private LocalDate bhytGiaTriDen;
        private String nguoiNhaHoTenDiaChi;
        private String nguoiNhaSoDienThoai;

        public Builder maBenhNhan(Long maBenhNhan) {
            this.maBenhNhan = maBenhNhan;
            return this;
        }

        public Builder nguoi(Nguoi nguoi) {
            if (nguoi != null) {
                this.maBenhNhan = nguoi.getMaNguoi();
            }
            return this;
        }

        public Builder maHis(String maHis) {
            this.maHis = maHis;
            return this;
        }

        public Builder ngheNghiep(String ngheNghiep) {
            this.ngheNghiep = ngheNghiep;
            return this;
        }

        public Builder danToc(String danToc) {
            this.danToc = danToc;
            return this;
        }

        public Builder bhytSoThe(String bhytSoThe) {
            this.bhytSoThe = bhytSoThe;
            return this;
        }

        public Builder bhytGiaTriDen(LocalDate bhytGiaTriDen) {
            this.bhytGiaTriDen = bhytGiaTriDen;
            return this;
        }

        public Builder nguoiNhaHoTenDiaChi(String nguoiNhaHoTenDiaChi) {
            this.nguoiNhaHoTenDiaChi = nguoiNhaHoTenDiaChi;
            return this;
        }

        public Builder nguoiNhaSoDienThoai(String nguoiNhaSoDienThoai) {
            this.nguoiNhaSoDienThoai = nguoiNhaSoDienThoai;
            return this;
        }

        public BenhNhan build() {
            return new BenhNhan(maBenhNhan, maHis, ngheNghiep, danToc, bhytSoThe, bhytGiaTriDen, nguoiNhaHoTenDiaChi, nguoiNhaSoDienThoai);
        }
    }
}
