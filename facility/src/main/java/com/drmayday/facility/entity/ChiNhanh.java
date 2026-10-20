package com.drmayday.facility.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "chinhanh")
public class ChiNhanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chi_nhanh")
    private Long maChiNhanh;

    @Column(name = "ten_chi_nhanh", nullable = false)
    private String tenChiNhanh;

    @Column(name = "dia_chi", nullable = false)
    private String diaChi;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    public ChiNhanh() {
    }

    public ChiNhanh(Long maChiNhanh, String tenChiNhanh, String diaChi, String soDienThoai, Boolean trangThai) {
        this.maChiNhanh = maChiNhanh;
        this.tenChiNhanh = tenChiNhanh;
        this.diaChi = diaChi;
        this.soDienThoai = soDienThoai;
        this.trangThai = trangThai != null ? trangThai : true;
    }

    public Long getMaChiNhanh() {
        return maChiNhanh;
    }

    public void setMaChiNhanh(Long maChiNhanh) {
        this.maChiNhanh = maChiNhanh;
    }

    public String getTenChiNhanh() {
        return tenChiNhanh;
    }

    public void setTenChiNhanh(String tenChiNhanh) {
        this.tenChiNhanh = tenChiNhanh;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public Boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Boolean trangThai) {
        this.trangThai = trangThai;
    }
}
