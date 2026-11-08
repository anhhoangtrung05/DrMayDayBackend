package com.example.drmayday.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "phongkham")
public class PhongKham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_phong_kham")
    private Long maPhongKham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_chi_nhanh", nullable = false)
    private ChiNhanh chiNhanh;

    @Column(name = "ten_phong_kham", nullable = false)
    private String tenPhongKham;

    public PhongKham() {
    }

    public PhongKham(Long maPhongKham, ChiNhanh chiNhanh, String tenPhongKham) {
        this.maPhongKham = maPhongKham;
        this.chiNhanh = chiNhanh;
        this.tenPhongKham = tenPhongKham;
    }

    public Long getMaPhongKham() {
        return maPhongKham;
    }

    public void setMaPhongKham(Long maPhongKham) {
        this.maPhongKham = maPhongKham;
    }

    public ChiNhanh getChiNhanh() {
        return chiNhanh;
    }

    public void setChiNhanh(ChiNhanh chiNhanh) {
        this.chiNhanh = chiNhanh;
    }

    public String getTenPhongKham() {
        return tenPhongKham;
    }

    public void setTenPhongKham(String tenPhongKham) {
        this.tenPhongKham = tenPhongKham;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long maPhongKham;
        private ChiNhanh chiNhanh;
        private String tenPhongKham;

        public Builder maPhongKham(Long maPhongKham) {
            this.maPhongKham = maPhongKham;
            return this;
        }

        public Builder chiNhanh(ChiNhanh chiNhanh) {
            this.chiNhanh = chiNhanh;
            return this;
        }

        public Builder tenPhongKham(String tenPhongKham) {
            this.tenPhongKham = tenPhongKham;
            return this;
        }

        public PhongKham build() {
            return new PhongKham(maPhongKham, chiNhanh, tenPhongKham);
        }
    }
}
