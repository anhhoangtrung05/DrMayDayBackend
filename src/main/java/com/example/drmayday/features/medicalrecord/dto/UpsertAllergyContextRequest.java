package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO upsert hoàn cảnh khởi phát dị ứng (API 43). Body là toàn bộ nội dung của phần này:
 * trường không gửi sẽ thành NULL. Các trường *_thoi_gian tính bằng giờ, từ lúc tiếp xúc tới lúc khởi phát.
 */
@Getter
@Setter
public class UpsertAllergyContextRequest extends UpsertSectionRequest {

    @JsonProperty("thuc_an_nghi_ngo") private String foodSuspect;
    @JsonProperty("thuc_an_thoi_gian") private BigDecimal foodHours;
    @JsonProperty("thuc_an_tung_bi") private Boolean foodReactedBefore;
    @JsonProperty("thuc_an_loai_tien_su") private String foodHistoryType;
    @JsonProperty("thuc_an_tien_su_ghi_ro") private String foodHistoryNote;

    @JsonProperty("thuoc_nghi_ngo") private String medicationSuspect;
    @JsonProperty("thuoc_thoi_gian") private BigDecimal medicationHours;
    @JsonProperty("thuoc_tung_bi") private Boolean medicationReactedBefore;
    @JsonProperty("thuoc_loai_tien_su") private String medicationHistoryType;
    @JsonProperty("thuoc_tien_su_ghi_ro") private String medicationHistoryNote;

    @JsonProperty("con_trung_nghi_ngo") private String insectSuspect;
    @JsonProperty("con_trung_thoi_gian") private BigDecimal insectHours;
    @JsonProperty("con_trung_tung_bi") private Boolean insectReactedBefore;
    @JsonProperty("con_trung_loai_tien_su") private String insectHistoryType;
    @JsonProperty("con_trung_tien_su_ghi_ro") private String insectHistoryNote;

    @JsonProperty("khac_nghi_ngo") private String otherSuspect;
    @JsonProperty("khac_thoi_gian") private BigDecimal otherHours;
    @JsonProperty("khac_tung_bi") private Boolean otherReactedBefore;
    @JsonProperty("khac_loai_tien_su") private String otherHistoryType;
    @JsonProperty("khac_tien_su_ghi_ro") private String otherHistoryNote;
}
