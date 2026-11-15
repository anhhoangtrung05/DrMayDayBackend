package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpsertSelfHistoryRequest {
    @NotNull(message = "2001")
    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("tien_su_co_dia") private Boolean tienSuCoDia;
    @JsonProperty("tien_su_co_dia_ghi_ro") private String tienSuCoDiaGhiRo;
    @JsonProperty("tien_su_tuyen_giap") private Boolean tienSuTuyenGiap;
    @JsonProperty("tien_su_tuyen_giap_ghi_ro") private String tienSuTuyenGiapGhiRo;
    @JsonProperty("tien_su_tu_mien") private Boolean tienSuTuMien;
    @JsonProperty("tien_su_tu_mien_ghi_ro") private String tienSuTuMienGhiRo;
    @JsonProperty("tien_su_di_ung") private Boolean tienSuDiUng;
    @JsonProperty("tien_su_di_ung_ghi_ro") private String tienSuDiUngGhiRo;
    @JsonProperty("tien_su_phan_ve") private Boolean tienSuPhanVe;
    @JsonProperty("tien_su_phan_ve_ghi_ro") private String tienSuPhanVeGhiRo;
    @JsonProperty("tien_su_viem_nhiem_man") private Boolean tienSuViemNhiemMan;
    @JsonProperty("tien_su_viem_nhiem_man_ghi_ro") private String tienSuViemNhiemManGhiRo;
    @JsonProperty("tien_su_benh_khac") private Boolean tienSuBenhKhac;
    @JsonProperty("tien_su_benh_khac_ghi_ro") private String tienSuBenhKhacGhiRo;
}
