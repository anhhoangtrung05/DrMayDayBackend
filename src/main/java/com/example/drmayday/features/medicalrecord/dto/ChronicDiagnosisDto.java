package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.ChronicUrticariaRecord;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Phần chẩn đoán của hồ sơ MAN (bảng benhanmaydayman), trả trong API 36.
 */
public record ChronicDiagnosisDto(
        @JsonProperty("chan_doan_chinh") String primaryDiagnosis,
        @JsonProperty("phan_nhom_csu") String csuSubtype,
        @JsonProperty("chung_da_ve_noi") Boolean dermographism,
        @JsonProperty("may_day_cholinergic") Boolean cholinergicUrticaria,
        @JsonProperty("may_day_do_lanh") Boolean coldUrticaria,
        @JsonProperty("may_day_adrenergic") Boolean adrenergicUrticaria,
        @JsonProperty("may_day_do_nuoc") Boolean aquagenicUrticaria,
        @JsonProperty("may_day_do_anh_sang") Boolean solarUrticaria,
        @JsonProperty("may_day_cam_ung_khac") Boolean otherInducibleUrticaria,
        @JsonProperty("may_day_cam_ung_khac_ghi_ro") String otherInducibleUrticariaDescription,
        @JsonProperty("bieu_hien_qua_trinh_benh") String diseaseCourseManifestation) {

    public static ChronicDiagnosisDto from(ChronicUrticariaRecord e) {
        if (e == null) {
            return null;
        }
        return new ChronicDiagnosisDto(
                e.getPrimaryDiagnosis(),
                e.getCsuSubtype(),
                e.getDermographism(),
                e.getCholinergicUrticaria(),
                e.getColdUrticaria(),
                e.getAdrenergicUrticaria(),
                e.getAquagenicUrticaria(),
                e.getSolarUrticaria(),
                e.getOtherInducibleUrticaria(),
                e.getOtherInducibleUrticariaDescription(),
                e.getDiseaseCourseManifestation());
    }
}
