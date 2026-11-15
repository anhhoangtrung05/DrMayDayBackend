package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LesionDetailResponse {

    @JsonProperty("lesion_detail_id")
    private String lesionDetailId;

    @JsonProperty("exam_id")
    private String examId;

    @JsonProperty("loai_benh_an")
    private String loaiBenhAn;

    // CAP fields
    @JsonProperty("vi_tri_ton_thuong")
    private String viTriTonThuong;

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

    @JsonProperty("ten_dac_diem")
    private String tenDacDiem;

    @JsonProperty("gia_tri_ghi_nhan")
    private String giaTriGhiNhan;

    @JsonProperty("vi_tri_ghi_nhan")
    private String viTriGhiNhan;

    @JsonProperty("ghi_chu")
    private String ghiChu;
}
