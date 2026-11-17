package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO upsert tình trạng nhiễm trùng (API 45, chỉ hồ sơ CAP). Body là toàn bộ nội dung của phần này.
 * Các trường *_thoi_gian và *_khoang_cach tính bằng ngày.
 */
@Getter
@Setter
public class UpsertInfectionStatusRequest extends UpsertSectionRequest {

    @JsonProperty("ho_hap_ho") private Boolean respiratoryCough;
    @JsonProperty("ho_hap_dau_hong") private Boolean respiratorySoreThroat;
    @JsonProperty("ho_hap_hat_hoi") private Boolean respiratorySneezing;
    @JsonProperty("ho_hap_so_mui") private Boolean respiratoryRunnyNose;
    @JsonProperty("ho_hap_thoi_gian") private BigDecimal respiratoryDays;
    @JsonProperty("ho_hap_khoang_cach") private BigDecimal respiratoryIntervalDays;

    @JsonProperty("tiet_nieu_tieu_rat") private Boolean urinaryPainful;
    @JsonProperty("tiet_nieu_tieu_buot") private Boolean urinaryBurning;
    @JsonProperty("tiet_nieu_thoi_gian") private BigDecimal urinaryDays;
    @JsonProperty("tiet_nieu_khoang_cach") private BigDecimal urinaryIntervalDays;

    @JsonProperty("da_mun") private Boolean skinAcne;
    @JsonProperty("da_nhot") private Boolean skinBoils;
    @JsonProperty("da_thoi_gian") private BigDecimal skinDays;
    @JsonProperty("da_khoang_cach") private BigDecimal skinIntervalDays;

    @JsonProperty("khac_ghi_ro") private String otherDescription;
    @JsonProperty("khac_thoi_gian") private BigDecimal otherDays;
    @JsonProperty("khac_khoang_cach") private BigDecimal otherIntervalDays;
}
