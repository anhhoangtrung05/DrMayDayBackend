package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.AllergyOnsetContext;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Hoàn cảnh khởi phát dị ứng (bảng hoancanhkhoiphatdiung), trả trong API 36.
 */
public record AllergyContextDto(
        @JsonProperty("thuc_an_nghi_ngo") String suspectedFoodTrigger,
        @JsonProperty("thuc_an_thoi_gian") BigDecimal foodTriggerDuration,
        @JsonProperty("thuc_an_tung_bi") Boolean previousFoodReaction,
        @JsonProperty("thuc_an_loai_tien_su") String foodHistoryType,
        @JsonProperty("thuc_an_tien_su_ghi_ro") String foodHistoryDescription,
        @JsonProperty("thuoc_nghi_ngo") String suspectedMedicationTrigger,
        @JsonProperty("thuoc_thoi_gian") BigDecimal medicationTriggerDuration,
        @JsonProperty("thuoc_tung_bi") Boolean previousMedicationReaction,
        @JsonProperty("thuoc_loai_tien_su") String medicationHistoryType,
        @JsonProperty("thuoc_tien_su_ghi_ro") String medicationHistoryDescription,
        @JsonProperty("con_trung_nghi_ngo") String suspectedInsectTrigger,
        @JsonProperty("con_trung_thoi_gian") BigDecimal insectTriggerDuration,
        @JsonProperty("con_trung_tung_bi") Boolean previousInsectReaction,
        @JsonProperty("con_trung_loai_tien_su") String insectHistoryType,
        @JsonProperty("con_trung_tien_su_ghi_ro") String insectHistoryDescription,
        @JsonProperty("khac_nghi_ngo") String suspectedOtherTrigger,
        @JsonProperty("khac_thoi_gian") BigDecimal otherDuration,
        @JsonProperty("khac_tung_bi") Boolean previousOtherReaction,
        @JsonProperty("khac_loai_tien_su") String otherHistoryType,
        @JsonProperty("khac_tien_su_ghi_ro") String otherHistoryDescription) {

    public static AllergyContextDto from(AllergyOnsetContext e) {
        if (e == null) {
            return null;
        }
        return new AllergyContextDto(
                e.getSuspectedFoodTrigger(),
                JsonValues.twoDecimals(e.getFoodTriggerDuration()),
                e.getPreviousFoodReaction(),
                e.getFoodHistoryType(),
                e.getFoodHistoryDescription(),
                e.getSuspectedMedicationTrigger(),
                JsonValues.twoDecimals(e.getMedicationTriggerDuration()),
                e.getPreviousMedicationReaction(),
                e.getMedicationHistoryType(),
                e.getMedicationHistoryDescription(),
                e.getSuspectedInsectTrigger(),
                JsonValues.twoDecimals(e.getInsectTriggerDuration()),
                e.getPreviousInsectReaction(),
                e.getInsectHistoryType(),
                e.getInsectHistoryDescription(),
                e.getSuspectedOtherTrigger(),
                JsonValues.twoDecimals(e.getOtherDuration()),
                e.getPreviousOtherReaction(),
                e.getOtherHistoryType(),
                e.getOtherHistoryDescription());
    }
}
