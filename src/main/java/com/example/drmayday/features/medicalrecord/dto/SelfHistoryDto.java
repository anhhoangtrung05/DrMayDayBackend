package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.PersonalMedicalHistory;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Tiền sử bản thân (bảng tiensubanthan), trả trong API 36.
 */
public record SelfHistoryDto(
        @JsonProperty("tien_su_co_dia") Boolean atopicHistory,
        @JsonProperty("tien_su_co_dia_ghi_ro") String atopicHistoryDescription,
        @JsonProperty("tien_su_tuyen_giap") Boolean thyroidHistory,
        @JsonProperty("tien_su_tuyen_giap_ghi_ro") String thyroidHistoryDescription,
        @JsonProperty("tien_su_tu_mien") Boolean autoimmuneHistory,
        @JsonProperty("tien_su_tu_mien_ghi_ro") String autoimmuneHistoryDescription,
        @JsonProperty("tien_su_di_ung") Boolean allergyHistory,
        @JsonProperty("tien_su_di_ung_ghi_ro") String allergyHistoryDescription,
        @JsonProperty("tien_su_phan_ve") Boolean anaphylaxisHistory,
        @JsonProperty("tien_su_phan_ve_ghi_ro") String anaphylaxisHistoryDescription,
        @JsonProperty("tien_su_viem_nhiem_man") Boolean chronicInfectionHistory,
        @JsonProperty("tien_su_viem_nhiem_man_ghi_ro") String chronicInfectionHistoryDescription,
        @JsonProperty("tien_su_benh_khac") Boolean otherMedicalHistory,
        @JsonProperty("tien_su_benh_khac_ghi_ro") String otherMedicalHistoryDescription) {

    public static SelfHistoryDto from(PersonalMedicalHistory e) {
        if (e == null) {
            return null;
        }
        return new SelfHistoryDto(
                e.getAtopicHistory(),
                e.getAtopicHistoryDescription(),
                e.getThyroidHistory(),
                e.getThyroidHistoryDescription(),
                e.getAutoimmuneHistory(),
                e.getAutoimmuneHistoryDescription(),
                e.getAllergyHistory(),
                e.getAllergyHistoryDescription(),
                e.getAnaphylaxisHistory(),
                e.getAnaphylaxisHistoryDescription(),
                e.getChronicInfectionHistory(),
                e.getChronicInfectionHistoryDescription(),
                e.getOtherMedicalHistory(),
                e.getOtherMedicalHistoryDescription());
    }
}
