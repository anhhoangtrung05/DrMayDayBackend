package com.example.drmayday.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "taikhoan")
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tai_khoan")
    private Long maTaiKhoan;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ma_nguoi", nullable = false)
    private Nguoi nguoi;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ma_vai_tro", nullable = false)
    private VaiTro vaiTro;

    @Column(name = "ten_dang_nhap", nullable = false, unique = true)
    private String tenDangNhap;

    @Column(name = "mat_khau_ma_hoa", nullable = false)
    private String matKhauMaHoa;

    @Column(name = "trang_thai_hoat_dong", nullable = false)
    private Boolean trangThaiHoatDong = true;

    @Column(name = "thoi_diem_tao", nullable = false)
    private OffsetDateTime thoiDiemTao;

    @Column(name = "thoi_diem_cap_nhat", nullable = false)
    private OffsetDateTime thoiDiemCapNhat;

    public TaiKhoan() {
    }

    public TaiKhoan(Long maTaiKhoan, Nguoi nguoi, VaiTro vaiTro, String tenDangNhap, String matKhauMaHoa,
                    Boolean trangThaiHoatDong, OffsetDateTime thoiDiemTao, OffsetDateTime thoiDiemCapNhat) {
        this.maTaiKhoan = maTaiKhoan;
        this.nguoi = nguoi;
        this.vaiTro = vaiTro;
        this.tenDangNhap = tenDangNhap;
        this.matKhauMaHoa = matKhauMaHoa;
        this.trangThaiHoatDong = trangThaiHoatDong != null ? trangThaiHoatDong : true;
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

    public Long getMaTaiKhoan() {
        return maTaiKhoan;
    }

    public void setMaTaiKhoan(Long maTaiKhoan) {
        this.maTaiKhoan = maTaiKhoan;
    }

    public Nguoi getNguoi() {
        return nguoi;
    }

    public void setNguoi(Nguoi nguoi) {
        this.nguoi = nguoi;
    }

    public VaiTro getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(VaiTro vaiTro) {
        this.vaiTro = vaiTro;
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    public String getMatKhauMaHoa() {
        return matKhauMaHoa;
    }

    public void setMatKhauMaHoa(String matKhauMaHoa) {
        this.matKhauMaHoa = matKhauMaHoa;
    }

    public Boolean getTrangThaiHoatDong() {
        return trangThaiHoatDong;
    }

    public void setTrangThaiHoatDong(Boolean trangThaiHoatDong) {
        this.trangThaiHoatDong = trangThaiHoatDong;
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
        private Long maTaiKhoan;
        private Nguoi nguoi;
        private VaiTro vaiTro;
        private String tenDangNhap;
        private String matKhauMaHoa;
        private Boolean trangThaiHoatDong = true;
        private OffsetDateTime thoiDiemTao;
        private OffsetDateTime thoiDiemCapNhat;

        public Builder maTaiKhoan(Long maTaiKhoan) {
            this.maTaiKhoan = maTaiKhoan;
            return this;
        }

        public Builder nguoi(Nguoi nguoi) {
            this.nguoi = nguoi;
            return this;
        }

        public Builder vaiTro(VaiTro vaiTro) {
            this.vaiTro = vaiTro;
            return this;
        }

        public Builder tenDangNhap(String tenDangNhap) {
            this.tenDangNhap = tenDangNhap;
            return this;
        }

        public Builder matKhauMaHoa(String matKhauMaHoa) {
            this.matKhauMaHoa = matKhauMaHoa;
            return this;
        }

        public Builder trangThaiHoatDong(Boolean trangThaiHoatDong) {
            this.trangThaiHoatDong = trangThaiHoatDong;
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

        public TaiKhoan build() {
            return new TaiKhoan(maTaiKhoan, nguoi, vaiTro, tenDangNhap, matKhauMaHoa, trangThaiHoatDong, thoiDiemTao, thoiDiemCapNhat);
        }
    }
}
