package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.AcuteUrticariaRecord;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Phần chẩn đoán của hồ sơ CAP (bảng benhanmaydaycap), trả trong API 36.
 */
public record AcuteDiagnosisDto(
        @JsonProperty("nhap_vien") Boolean hospitalized,
        @JsonProperty("cd_soc_phan_ve") Boolean diagnosedAnaphylacticShock,
        @JsonProperty("cd_phan_ve") Boolean diagnosedAnaphylaxis,
        @JsonProperty("cd_may_day_cap_nang") Boolean diagnosedSevereAcuteUrticaria,
        @JsonProperty("cd_may_day_cap_thong_thuong") Boolean diagnosedTypicalAcuteUrticaria,
        @JsonProperty("tien_su_benh_nen_da") String skinComorbidityHistory,
        @JsonProperty("tien_su_benh_nen_ho_hap") String respiratoryComorbidityHistory,
        @JsonProperty("tien_su_benh_nen_tuan_hoan") String circulatoryComorbidityHistory,
        @JsonProperty("tien_su_benh_tim_mach_ghi_ro") String cardiovascularHistoryDescription,
        @JsonProperty("tien_su_benh_tieu_hoa_man_tinh_ghi_ro") String chronicGastrointestinalHistoryDescription,
        @JsonProperty("tien_su_benh_nen_tieu_hoa") String gastrointestinalComorbidityHistory,
        @JsonProperty("nhom_yeu_to_nghi_ngo") String suspectedTriggerGroup,
        @JsonProperty("co_nghi_ngo_dot_nay") Boolean suspectedTriggerInCurrentEpisode,
        @JsonProperty("ten_yeu_to_nghi_ngo") String suspectedTriggerName,
        @JsonProperty("loai_tien_su") String historyType,
        @JsonProperty("da_tung_phan_ung_truoc_day") Boolean previousReaction,
        @JsonProperty("thoi_gian_tiep_xuc_den_khoi_phat") String exposureToOnsetDuration,
        @JsonProperty("thoi_gian_keo_dai_tb") String averageDuration,
        @JsonProperty("dot_gan_nhat_cach") String timeSinceLastEpisode,
        @JsonProperty("tien_su_phan_ve_ghi_ro") String anaphylaxisHistoryDescription,
        @JsonProperty("tien_su_di_ung_ghi_ro") String allergyHistoryDescription,
        @JsonProperty("da_dieu_tri_benh_dot_nay") Boolean treatedCurrentEpisode,
        @JsonProperty("tuoi_lan_dau_bi_benh") Integer ageAtFirstOnset,
        @JsonProperty("so_lan_may_day_cap_truoc") Integer previousAcuteUrticariaCount,
        @JsonProperty("thoi_diem_cap_nhat") String updatedAt,
        @JsonProperty("benh_phu") String comorbidities) {

    public static AcuteDiagnosisDto from(AcuteUrticariaRecord e) {
        if (e == null) {
            return null;
        }
        return new AcuteDiagnosisDto(
                e.getHospitalized(),
                e.getDiagnosedAnaphylacticShock(),
                e.getDiagnosedAnaphylaxis(),
                e.getDiagnosedSevereAcuteUrticaria(),
                e.getDiagnosedTypicalAcuteUrticaria(),
                e.getSkinComorbidityHistory(),
                e.getRespiratoryComorbidityHistory(),
                e.getCirculatoryComorbidityHistory(),
                e.getCardiovascularHistoryDescription(),
                e.getChronicGastrointestinalHistoryDescription(),
                e.getGastrointestinalComorbidityHistory(),
                e.getSuspectedTriggerGroup(),
                e.getSuspectedTriggerInCurrentEpisode(),
                e.getSuspectedTriggerName(),
                e.getHistoryType(),
                e.getPreviousReaction(),
                JsonValues.duration(e.getExposureToOnsetDuration()),
                JsonValues.duration(e.getAverageDuration()),
                JsonValues.duration(e.getTimeSinceLastEpisode()),
                e.getAnaphylaxisHistoryDescription(),
                e.getAllergyHistoryDescription(),
                e.getTreatedCurrentEpisode(),
                e.getAgeAtFirstOnset(),
                e.getPreviousAcuteUrticariaCount(),
                JsonValues.timestamp(e.getUpdatedAt()),
                e.getComorbidities());
    }
}
