package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpsertChronicDiagnosisRequest {
    @NotNull(message = "2001")
    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("chan_doan_chinh") private String chanDoanChinh;
    @JsonProperty("phan_nhom_csu") private String phanNhomCsu;
    @JsonProperty("chung_da_ve_noi") private Boolean chungDaVeNoi;
    @JsonProperty("may_day_cholinergic") private Boolean mayDayCholinergic;
    @JsonProperty("may_day_do_lanh") private Boolean mayDayDoLanh;
    @JsonProperty("may_day_adrenergic") private Boolean mayDayAdrenergic;
    @JsonProperty("may_day_do_nuoc") private Boolean mayDayDoNuoc;
    @JsonProperty("may_day_do_anh_sang") private Boolean mayDayDoAnhSang;
    @JsonProperty("may_day_cam_ung_khac") private Boolean mayDayCamUngKhac;
    @JsonProperty("may_day_cam_ung_khac_ghi_ro") private String mayDayCamUngKhacGhiRo;
    @JsonProperty("bieu_hien_qua_trinh_benh") private String bieuHienQuaTrinhBenh;
}
