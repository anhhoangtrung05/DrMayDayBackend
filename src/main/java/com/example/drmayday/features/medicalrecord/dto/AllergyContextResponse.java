package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.AllergyOnsetContext;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Phản hồi API 43: record_id, khóa chính của dòng, rồi mọi cột theo cùng thứ tự với allergy_context của API 36.
 */
public record AllergyContextResponse(
        @JsonProperty("record_id") String recordId,
        @JsonProperty("ma_hoan_canh_khoi_phat") String id,
        @JsonProperty("thuc_an_nghi_ngo") String foodSuspect,
        @JsonProperty("thuc_an_thoi_gian") BigDecimal foodHours,
        @JsonProperty("thuc_an_tung_bi") Boolean foodReactedBefore,
        @JsonProperty("thuc_an_loai_tien_su") String foodHistoryType,
        @JsonProperty("thuc_an_tien_su_ghi_ro") String foodHistoryNote,
        @JsonProperty("thuoc_nghi_ngo") String medicationSuspect,
        @JsonProperty("thuoc_thoi_gian") BigDecimal medicationHours,
        @JsonProperty("thuoc_tung_bi") Boolean medicationReactedBefore,
        @JsonProperty("thuoc_loai_tien_su") String medicationHistoryType,
        @JsonProperty("thuoc_tien_su_ghi_ro") String medicationHistoryNote,
        @JsonProperty("con_trung_nghi_ngo") String insectSuspect,
        @JsonProperty("con_trung_thoi_gian") BigDecimal insectHours,
        @JsonProperty("con_trung_tung_bi") Boolean insectReactedBefore,
        @JsonProperty("con_trung_loai_tien_su") String insectHistoryType,
        @JsonProperty("con_trung_tien_su_ghi_ro") String insectHistoryNote,
        @JsonProperty("khac_nghi_ngo") String otherSuspect,
        @JsonProperty("khac_thoi_gian") BigDecimal otherHours,
        @JsonProperty("khac_tung_bi") Boolean otherReactedBefore,
        @JsonProperty("khac_loai_tien_su") String otherHistoryType,
        @JsonProperty("khac_tien_su_ghi_ro") String otherHistoryNote) {

    public static AllergyContextResponse from(AllergyOnsetContext e) {
        return new AllergyContextResponse(
                JsonValues.id(e.getMedicalRecordId()),
                JsonValues.id(e.getAllergyOnsetContextId()),
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
