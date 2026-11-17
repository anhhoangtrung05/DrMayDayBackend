package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.PreviousDiseaseEpisode;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Bệnh sử các đợt bệnh trước (bảng benhsucacdotbenhtruoc, chỉ hồ sơ MAN), trả trong API 36.
 */
public record PreviousEpisodesDto(
        @JsonProperty("tung_co_dot_tuong_tu") Boolean previousSimilarEpisodes,
        @JsonProperty("tuoi_khoi_phat") Short onsetAge,
        @JsonProperty("so_dot_truoc") Integer previousEpisodeCount,
        @JsonProperty("kieu_dien_bien") String diseaseCoursePattern,
        @JsonProperty("bieu_hien_dot_truoc") String previousEpisodeManifestation,
        @JsonProperty("da_tung_nhap_vien") Boolean previouslyHospitalized,
        @JsonProperty("so_lan_nhap_vien") Integer hospitalizationCount,
        @JsonProperty("thoi_gian_tu_dot_gan_nhat") BigDecimal timeSinceLastEpisode,
        @JsonProperty("dien_bien_cac_dot_truoc") String previousEpisodeCourse) {

    public static PreviousEpisodesDto from(PreviousDiseaseEpisode e) {
        if (e == null) {
            return null;
        }
        return new PreviousEpisodesDto(
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
