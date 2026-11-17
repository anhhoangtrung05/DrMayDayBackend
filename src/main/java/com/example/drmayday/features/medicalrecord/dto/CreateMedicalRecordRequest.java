package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateMedicalRecordRequest {
    @NotNull(message = "2001")
    @JsonProperty("patient_id")
    private Long patientId;

    @NotBlank(message = "2001")
    @JsonProperty("loai_benh_an")
    private String loaiBenhAn;

    @JsonProperty("trang_thai")
    private String trangThai;

    @JsonProperty("chan_doan_tuyen_truoc")
    private String chanDoanTuyenTruoc;

    @JsonProperty("chan_doan_ban_dau_phong_kham")
    private String chanDoanBanDauPhongKham;

    @JsonProperty("thoi_gian_khoi_phat_tuan")
    private BigDecimal thoiGianKhoiPhatTuan;

    @JsonProperty("thoi_diem_kham_lan_dau")
    private String thoiDiemKhamLanDau;

    @JsonProperty("ngay_bat_dau_dieu_tri_ngoai_tru")
    private String ngayBatDauDieuTriNgoaiTru;

    @JsonProperty("ngay_ket_thuc_dieu_tri_ngoai_tru")
    private String ngayKetThucDieuTriNgoaiTru;

    @JsonProperty("ket_qua_dieu_tri_bao_hiem")
    private String ketQuaDieuTriBaoHiem;

    @JsonProperty("tien_su_gia_dinh")
    private String tienSuGiaDinh;

    @JsonProperty("tien_su_gia_dinh_ghi_ro")
    private String tienSuGiaDinhGhiRo;

    @JsonProperty("da_dieu_tri_truoc_kham")
    private Boolean daDieuTriTruocKham;
}
