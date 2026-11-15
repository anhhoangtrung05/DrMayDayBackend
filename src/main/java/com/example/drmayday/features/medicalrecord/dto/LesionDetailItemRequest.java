package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LesionDetailItemRequest {

    // CAP fields
    @JsonProperty("vi_tri_ton_thuong")
    private String viTriTonThuong;

    @JsonProperty("vi_tri")
    private String viTri;

    @JsonProperty("loai_ton_thuong")
    private String loaiTonThuong;

    @JsonProperty("kich_thuoc_mm")
    private BigDecimal kichThuocMm;

    @JsonProperty("mau_sac")
    private String mauSac;

    @JsonProperty("thoi_gian_ton_tai_gio")
    private BigDecimal thoiGianTonTaiGio;

    @JsonProperty("mo_ta_chi_tiet")
    private String moTaChiTiet;

    // MAN fields
    @JsonProperty("ma_dac_diem_san_phu")
    private String maDacDiemSanPhu;

    @JsonProperty("gia_tri_ghi_nhan")
    private String giaTriGhiNhan;

    @JsonProperty("vi_tri_ghi_nhan")
    private String viTriGhiNhan;

    @JsonProperty("ghi_chu")
    private String ghiChu;
}
