package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.PreviousDiseaseEpisode;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Phản hồi API 44: record_id, khóa chính của dòng, rồi mọi cột theo cùng thứ tự với previous_episodes của API 36.
 */
public record PreviousEpisodesResponse(
        @JsonProperty("record_id") String recordId,
        @JsonProperty("ma_tien_su") String id,
        @JsonProperty("tung_co_dot_tuong_tu") Boolean hadSimilarEpisodes,
        @JsonProperty("tuoi_khoi_phat") Short onsetAge,
        @JsonProperty("so_dot_truoc") Integer previousEpisodeCount,
        @JsonProperty("kieu_dien_bien") String coursePattern,
        @JsonProperty("bieu_hien_dot_truoc") String previousManifestation,
        @JsonProperty("da_tung_nhap_vien") Boolean wasHospitalized,
        @JsonProperty("so_lan_nhap_vien") Integer hospitalizationCount,
        @JsonProperty("thoi_gian_tu_dot_gan_nhat") BigDecimal monthsSinceLastEpisode,
        @JsonProperty("dien_bien_cac_dot_truoc") String previousEpisodeCourse) {

    public static PreviousEpisodesResponse from(PreviousDiseaseEpisode e) {
        return new PreviousEpisodesResponse(
                JsonValues.id(e.getMedicalRecordId()),
                JsonValues.id(e.getPreviousDiseaseEpisodeId()),
                e.getPreviousSimilarEpisodes(),
                e.getOnsetAge(),
                e.getPreviousEpisodeCount(),
                e.getDiseaseCoursePattern(),
                e.getPreviousEpisodeManifestation(),
                e.getPreviouslyHospitalized(),
                e.getHospitalizationCount(),
                JsonValues.twoDecimals(e.getTimeSinceLastEpisode()),
                e.getPreviousEpisodeCourse());
    }
}
