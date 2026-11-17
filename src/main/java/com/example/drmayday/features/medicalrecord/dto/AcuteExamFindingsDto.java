package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.AcuteVisit;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Nội dung khám của phiên CAP (bảng phienkhamcap), trả trong API 48. Mọi cột trừ ma_phien_kham và loai_benh_an.
 */
public record AcuteExamFindingsDto(
        @JsonProperty("trieu_chung_da") String skinSymptoms,
        @JsonProperty("trieu_chung_ho_hap") String respiratorySymptoms,
        @JsonProperty("so_voi_thuong_ngay_ho_hap") String respiratorySymptomsComparedToUsual,
        @JsonProperty("trieu_chung_tuan_hoan") String cardiovascularSymptoms,
        @JsonProperty("so_voi_thuong_ngay_tuan_hoan") String cardiovascularSymptomsComparedToUsual,
        @JsonProperty("trieu_chung_tieu_hoa") String gastrointestinalSymptoms,
        @JsonProperty("so_voi_thuong_ngay_tieu_hoa") String gastrointestinalSymptomsComparedToUsual,
        @JsonProperty("co_san_phu") Boolean hasWheals,
        @JsonProperty("co_anh_san_phu") Boolean hasWhealPhotos,
        @JsonProperty("so_lan_non") Integer vomitingEpisodes) {

    public static AcuteExamFindingsDto from(AcuteVisit e) {
        if (e == null) {
            return null;
        }
        return new AcuteExamFindingsDto(
                e.getSkinSymptoms(),
                e.getRespiratorySymptoms(),
                e.getRespiratorySymptomsComparedToUsual(),
                e.getCardiovascularSymptoms(),
                e.getCardiovascularSymptomsComparedToUsual(),
                e.getGastrointestinalSymptoms(),
                e.getGastrointestinalSymptomsComparedToUsual(),
                e.getHasWheals(),
                e.getHasWhealPhotos(),
                e.getVomitingEpisodes());
    }
}
