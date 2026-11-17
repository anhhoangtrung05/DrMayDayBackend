package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO upsert bệnh sử các đợt bệnh trước (API 44, chỉ hồ sơ MAN). Body là toàn bộ nội dung của phần này.
 * thoi_gian_tu_dot_gan_nhat tính bằng tháng.
 */
@Getter
@Setter
public class UpsertPreviousEpisodesRequest extends UpsertSectionRequest {

    @JsonProperty("tung_co_dot_tuong_tu") private Boolean hadSimilarEpisodes;
    @JsonProperty("tuoi_khoi_phat") private Short onsetAge;
    @JsonProperty("so_dot_truoc") private Integer previousEpisodeCount;
    @JsonProperty("kieu_dien_bien") private String coursePattern;
    @JsonProperty("bieu_hien_san_phu") private Boolean whealManifestation;
    @JsonProperty("bieu_hien_phu_mach") private Boolean angioedemaManifestation;
    @JsonProperty("bieu_hien_khac_ghi_ro") private String otherManifestation;
    // 'Có' | 'Không' | 'Không rõ', đúng giá trị của ck_benh_su_da_tung_nhap_vien
    @JsonProperty("da_tung_nhap_vien") private String hospitalized;
    @JsonProperty("so_lan_nhap_vien") private Integer hospitalizationCount;
    @JsonProperty("thoi_gian_tu_dot_gan_nhat") private BigDecimal monthsSinceLastEpisode;
    @JsonProperty("dien_bien_cac_dot_truoc") private String previousEpisodeCourse;
}
