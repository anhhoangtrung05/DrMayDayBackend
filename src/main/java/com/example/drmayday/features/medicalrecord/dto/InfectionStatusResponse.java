package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.InfectionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Phản hồi API 45: record_id, khóa chính của dòng, rồi mọi cột theo cùng thứ tự với infection_status của API 36.
 */
public record InfectionStatusResponse(
        @JsonProperty("record_id") String recordId,
        @JsonProperty("ma_tinh_trang_nhiem_trung") String id,
        @JsonProperty("ho_hap_ho") Boolean respiratoryCough,
        @JsonProperty("ho_hap_dau_hong") Boolean respiratorySoreThroat,
        @JsonProperty("ho_hap_hat_hoi") Boolean respiratorySneezing,
        @JsonProperty("ho_hap_so_mui") Boolean respiratoryRunnyNose,
        @JsonProperty("ho_hap_thoi_gian") BigDecimal respiratoryDays,
        @JsonProperty("ho_hap_khoang_cach") BigDecimal respiratoryIntervalDays,
        @JsonProperty("tiet_nieu_tieu_rat") Boolean urinaryPainful,
        @JsonProperty("tiet_nieu_tieu_buot") Boolean urinaryBurning,
        @JsonProperty("tiet_nieu_thoi_gian") BigDecimal urinaryDays,
        @JsonProperty("tiet_nieu_khoang_cach") BigDecimal urinaryIntervalDays,
        @JsonProperty("da_mun") Boolean skinAcne,
        @JsonProperty("da_nhot") Boolean skinBoils,
        @JsonProperty("da_thoi_gian") BigDecimal skinDays,
        @JsonProperty("da_khoang_cach") BigDecimal skinIntervalDays,
        @JsonProperty("khac_ghi_ro") String otherDescription,
        @JsonProperty("khac_thoi_gian") BigDecimal otherDays,
        @JsonProperty("khac_khoang_cach") BigDecimal otherIntervalDays) {

    public static InfectionStatusResponse from(InfectionStatus e) {
        return new InfectionStatusResponse(
                JsonValues.id(e.getMedicalRecordId()),
                JsonValues.id(e.getInfectionStatusId()),
                e.getRespiratoryCough(),
                e.getRespiratorySoreThroat(),
                e.getRespiratorySneezing(),
                e.getRespiratoryRunnyNose(),
                JsonValues.twoDecimals(e.getRespiratoryDuration()),
                JsonValues.twoDecimals(e.getRespiratoryInterval()),
                e.getUrinaryPainfulUrination(),
                e.getUrinaryBurningUrination(),
                JsonValues.twoDecimals(e.getUrinaryDuration()),
                JsonValues.twoDecimals(e.getUrinaryInterval()),
                e.getSkinAcne(),
                e.getSkinBoils(),
                JsonValues.twoDecimals(e.getSkinDuration()),
                JsonValues.twoDecimals(e.getSkinInterval()),
                e.getOtherDescription(),
                JsonValues.twoDecimals(e.getOtherDuration()),
                JsonValues.twoDecimals(e.getOtherInterval()));
    }
}
