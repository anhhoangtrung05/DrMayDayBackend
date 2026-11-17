package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.InfectionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Tình trạng nhiễm trùng (bảng tinhtrangnhiemtrung, chỉ hồ sơ CAP), trả trong API 36.
 */
public record InfectionStatusDto(
        @JsonProperty("ho_hap_ho") Boolean respiratoryCough,
        @JsonProperty("ho_hap_dau_hong") Boolean respiratorySoreThroat,
        @JsonProperty("ho_hap_hat_hoi") Boolean respiratorySneezing,
        @JsonProperty("ho_hap_so_mui") Boolean respiratoryRunnyNose,
        @JsonProperty("ho_hap_thoi_gian") BigDecimal respiratoryDuration,
        @JsonProperty("ho_hap_khoang_cach") BigDecimal respiratoryInterval,
        @JsonProperty("tiet_nieu_tieu_rat") Boolean urinaryPainfulUrination,
        @JsonProperty("tiet_nieu_tieu_buot") Boolean urinaryBurningUrination,
        @JsonProperty("tiet_nieu_thoi_gian") BigDecimal urinaryDuration,
        @JsonProperty("tiet_nieu_khoang_cach") BigDecimal urinaryInterval,
        @JsonProperty("da_mun") Boolean skinAcne,
        @JsonProperty("da_nhot") Boolean skinBoils,
        @JsonProperty("da_thoi_gian") BigDecimal skinDuration,
        @JsonProperty("da_khoang_cach") BigDecimal skinInterval,
        @JsonProperty("khac_ghi_ro") String otherDescription,
        @JsonProperty("khac_thoi_gian") BigDecimal otherDuration,
        @JsonProperty("khac_khoang_cach") BigDecimal otherInterval) {

    public static InfectionStatusDto from(InfectionStatus e) {
        if (e == null) {
            return null;
        }
        return new InfectionStatusDto(
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
