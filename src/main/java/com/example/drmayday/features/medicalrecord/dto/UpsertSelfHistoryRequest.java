package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpsertSelfHistoryRequest {
    /** Taken from the path, not the body. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long recordId;

    @JsonProperty("tien_su_co_dia") private String tienSuCoDia;
    @JsonProperty("co_dia_viem_da_co_dia") private Boolean coDiaViemDaCoDia;
    @JsonProperty("co_dia_hen") private Boolean coDiaHen;
    @JsonProperty("co_dia_viem_mui_di_ung") private Boolean coDiaViemMuiDiUng;
    @JsonProperty("co_dia_khac") private Boolean coDiaKhac;
    @JsonProperty("co_dia_khac_ghi_ro") private String coDiaKhacGhiRo;

    @JsonProperty("tien_su_tuyen_giap") private Boolean tienSuTuyenGiap;
    @JsonProperty("tien_su_tuyen_giap_ghi_ro") private String tienSuTuyenGiapGhiRo;

    @JsonProperty("tien_su_tu_mien") private Boolean tienSuTuMien;
    @JsonProperty("tien_su_tu_mien_ghi_ro") private String tienSuTuMienGhiRo;

    @JsonProperty("tien_su_di_ung") private String tienSuDiUng;
    @JsonProperty("di_ung_thuoc") private Boolean diUngThuoc;
    @JsonProperty("di_ung_thuoc_ghi_ro") private String diUngThuocGhiRo;
    @JsonProperty("di_ung_thuc_an") private Boolean diUngThucAn;
    @JsonProperty("di_ung_thuc_an_ghi_ro") private String diUngThucAnGhiRo;
    @JsonProperty("di_ung_di_nguyen_khac") private Boolean diUngDiNguyenKhac;
    @JsonProperty("di_ung_di_nguyen_khac_ghi_ro") private String diUngDiNguyenKhacGhiRo;

    @JsonProperty("tien_su_phan_ve") private Boolean tienSuPhanVe;
    @JsonProperty("tien_su_phan_ve_ghi_ro") private String tienSuPhanVeGhiRo;

    @JsonProperty("tien_su_viem_nhiem_man") private Boolean tienSuViemNhiemMan;
    @JsonProperty("tien_su_viem_nhiem_man_ghi_ro") private String tienSuViemNhiemManGhiRo;

    @JsonProperty("tien_su_benh_khac") private Boolean tienSuBenhKhac;
    @JsonProperty("tien_su_benh_khac_ghi_ro") private String tienSuBenhKhacGhiRo;
}
