package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.ChronicUrticariaRecord;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChronicDiagnosisResponse {
    @JsonProperty("record_id") private Long recordId;
    @JsonProperty("loai_benh_an") private String loaiBenhAn;
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

    public static ChronicDiagnosisResponse from(ChronicUrticariaRecord r) {
        return ChronicDiagnosisResponse.builder()
                .recordId(r.getMedicalRecordId())
                .loaiBenhAn("MAN")
                .chanDoanChinh(r.getPrimaryDiagnosis())
                .phanNhomCsu(r.getCsuSubtype())
                .chungDaVeNoi(r.getDermographism())
                .mayDayCholinergic(r.getCholinergicUrticaria())
                .mayDayDoLanh(r.getColdUrticaria())
                .mayDayAdrenergic(r.getAdrenergicUrticaria())
                .mayDayDoNuoc(r.getAquagenicUrticaria())
                .mayDayDoAnhSang(r.getSolarUrticaria())
                .mayDayCamUngKhac(r.getOtherInducibleUrticaria())
                .mayDayCamUngKhacGhiRo(r.getOtherInducibleUrticariaDescription())
                .bieuHienQuaTrinhBenh(r.getDiseaseCourseManifestation())
                .build();
    }
}
