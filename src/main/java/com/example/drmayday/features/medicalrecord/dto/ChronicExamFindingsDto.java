package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.ChronicVisit;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Nội dung khám của phiên MAN (bảng phienkhamman), trả trong API 48. Mọi cột trừ ma_phien_kham và loai_benh_an.
 */
public record ChronicExamFindingsDto(
        @JsonProperty("hien_tai_co_san_phu") Boolean currentlyHasWheals,
        @JsonProperty("uu_the_ngau_nhien") Boolean spontaneousPredominance,
        @JsonProperty("vi_tri_san_phu") String whealLocations,
        @JsonProperty("yeu_to_lam_nang_sa_phu") String papularUrticariaAggravatingFactor,
        @JsonProperty("hoan_canh_xuat_hien_san_phu") String whealOnsetContext,
        @JsonProperty("trong_dot_co_phu_mach") Boolean angioedemaDuringEpisode,
        @JsonProperty("hien_tai_co_phu_mach") Boolean currentlyHasAngioedema,
        @JsonProperty("so_lan_phu_mach_dot_nay") Integer angioedemaEpisodesInCurrentPeriod,
        @JsonProperty("hoan_canh_xuat_hien_phu_mach") String angioedemaOnsetContext,
        @JsonProperty("so_lan_phu_mach_nang_phai_nhap_vien") Integer angioedemaHospitalizations,
        @JsonProperty("thoi_gian_ton_tai_phu_mach_co_dung_thuoc") String angioedemaDurationWithMedication,
        @JsonProperty("thoi_gian_ton_tai_phu_mach_khong_dung_thuoc") String angioedemaDurationWithoutMedication,
        @JsonProperty("diem_aas7") Short aas7Score,
        @JsonProperty("ben_phu_mach") String angioedemaSide,
        @JsonProperty("thoi_gian_ngau_nhien_tuan") BigDecimal spontaneousDurationWeeks,
        @JsonProperty("uu_the_kich_thich") Boolean induciblePredominance,
        @JsonProperty("thoi_gian_kich_thich_tuan") BigDecimal inducibleDurationWeeks,
        @JsonProperty("uu_the_tuong_duong") Boolean equivalentPredominance,
        @JsonProperty("uu_the_khong_ro") Boolean predominanceUnknown,
        @JsonProperty("thu_tu_xuat_hien_so_voi_san_phu") String onsetOrderComparedToWheals,
        @JsonProperty("phu_mach_1_tuan_gan_day") Boolean angioedemaWithinLastWeek,
        @JsonProperty("chan_doan_tai_kham") String followUpDiagnosis,
        @JsonProperty("khoang_cach_xuat_hien") String onsetInterval,
        @JsonProperty("phu_mi_mat") Boolean eyelidEdema,
        @JsonProperty("phu_luoi") Boolean tongueEdema,
        @JsonProperty("phu_moi") Boolean lipEdema,
        @JsonProperty("phu_vung_mat_khac") Boolean otherFacialEdema,
        @JsonProperty("phu_thanh_quan") Boolean laryngealEdema,
        @JsonProperty("phu_ban_chan") Boolean footEdema,
        @JsonProperty("phu_sinh_duc") Boolean genitalEdema,
        @JsonProperty("phu_vi_tri_khac") Boolean edemaAtOtherLocation,
        @JsonProperty("phu_vi_tri_khac_ghi_ro") String otherEdemaLocationDescription,
        @JsonProperty("phu_ban_tay") Boolean handEdema,
        @JsonProperty("bat_thuong_co_quan_khac") Boolean otherOrganAbnormality,
        @JsonProperty("bat_thuong_co_quan_khac_ghi_ro") String otherOrganAbnormalityDescription) {

    public static ChronicExamFindingsDto from(ChronicVisit e) {
        if (e == null) {
            return null;
        }
        return new ChronicExamFindingsDto(
                e.getCurrentlyHasWheals(),
                e.getSpontaneousPredominance(),
                e.getWhealLocations(),
                e.getPapularUrticariaAggravatingFactor(),
                e.getWhealOnsetContext(),
                e.getAngioedemaDuringEpisode(),
                e.getCurrentlyHasAngioedema(),
                e.getAngioedemaEpisodesInCurrentPeriod(),
                e.getAngioedemaOnsetContext(),
                e.getAngioedemaHospitalizations(),
                e.getAngioedemaDurationWithMedication(),
                e.getAngioedemaDurationWithoutMedication(),
                e.getAas7Score(),
                e.getAngioedemaSide(),
                JsonValues.twoDecimals(e.getSpontaneousDurationWeeks()),
                e.getInduciblePredominance(),
                JsonValues.twoDecimals(e.getInducibleDurationWeeks()),
                e.getEquivalentPredominance(),
                e.getPredominanceUnknown(),
                e.getOnsetOrderComparedToWheals(),
                e.getAngioedemaWithinLastWeek(),
                e.getFollowUpDiagnosis(),
                e.getOnsetInterval(),
                e.getEyelidEdema(),
                e.getTongueEdema(),
                e.getLipEdema(),
                e.getOtherFacialEdema(),
                e.getLaryngealEdema(),
                e.getFootEdema(),
                e.getGenitalEdema(),
                e.getEdemaAtOtherLocation(),
                e.getOtherEdemaLocationDescription(),
                e.getHandEdema(),
                e.getOtherOrganAbnormality(),
                e.getOtherOrganAbnormalityDescription());
    }
}
