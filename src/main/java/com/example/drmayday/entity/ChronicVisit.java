package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "phienkhamman")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChronicVisit {
    @Id
    @Column(name = "ma_phien_kham", nullable = false) private Long medicalVisitId;
    @Column(name = "loai_benh_an", insertable = false, updatable = false) private String medicalRecordType;
    @Column(name = "hien_tai_co_san_phu") private Boolean currentlyHasWheals;
    @Column(name = "uu_the_ngau_nhien") private Boolean spontaneousPredominance;
    @Column(name = "vi_tri_san_phu") private String whealLocations;
    @Column(name = "yeu_to_lam_nang_sa_phu") private String papularUrticariaAggravatingFactor;
    @Column(name = "hoan_canh_xuat_hien_san_phu") private String whealOnsetContext;
    @Column(name = "trong_dot_co_phu_mach") private Boolean angioedemaDuringEpisode;
    @Column(name = "hien_tai_co_phu_mach") private Boolean currentlyHasAngioedema;
    @Column(name = "so_lan_phu_mach_dot_nay") private Integer angioedemaEpisodesInCurrentPeriod;
    @Column(name = "hoan_canh_xuat_hien_phu_mach") private String angioedemaOnsetContext;
    @Column(name = "so_lan_phu_mach_nang_phai_nhap_vien") private Integer angioedemaHospitalizations;
    @Column(name = "diem_aas7") private Short aas7Score;
    @Column(name = "ben_phu_mach") private String angioedemaSide;
    @Column(name = "thoi_gian_ngau_nhien_tuan", precision = 12, scale = 2) private BigDecimal spontaneousDurationWeeks;
    @Column(name = "uu_the_kich_thich") private Boolean induciblePredominance;
    @Column(name = "thoi_gian_kich_thich_tuan", precision = 12, scale = 2) private BigDecimal inducibleDurationWeeks;
    @Column(name = "uu_the_tuong_duong") private Boolean equivalentPredominance;
    @Column(name = "uu_the_khong_ro") private Boolean predominanceUnknown;
    @Column(name = "thu_tu_xuat_hien_so_voi_san_phu") private String onsetOrderComparedToWheals;
    @Column(name = "phu_mach_1_tuan_gan_day") private Boolean angioedemaWithinLastWeek;
    @Column(name = "chan_doan_tai_kham") private String followUpDiagnosis;
    @Column(name = "khoang_cach_xuat_hien") private String onsetInterval;
    @Column(name = "phu_mi_mat") private Boolean eyelidEdema;
    @Column(name = "phu_luoi") private Boolean tongueEdema;
    @Column(name = "phu_moi") private Boolean lipEdema;
    @Column(name = "phu_vung_mat_khac") private Boolean otherFacialEdema;
    @Column(name = "phu_thanh_quan") private Boolean laryngealEdema;
    @Column(name = "phu_ban_chan") private Boolean footEdema;
    @Column(name = "phu_sinh_duc") private Boolean genitalEdema;
    @Column(name = "phu_vi_tri_khac") private Boolean edemaAtOtherLocation;
    @Column(name = "phu_vi_tri_khac_ghi_ro") private String otherEdemaLocationDescription;
    @Column(name = "phu_ban_tay") private Boolean handEdema;
    @Column(name = "bat_thuong_co_quan_khac") private Boolean otherOrganAbnormality;
    @Column(name = "bat_thuong_co_quan_khac_ghi_ro") private String otherOrganAbnormalityDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "ma_phien_kham", referencedColumnName = "ma_phien_kham", insertable = false, updatable = false),
        @JoinColumn(name = "loai_benh_an", referencedColumnName = "loai_benh_an", insertable = false, updatable = false)
    })
    private MedicalVisit medicalVisit;
}
