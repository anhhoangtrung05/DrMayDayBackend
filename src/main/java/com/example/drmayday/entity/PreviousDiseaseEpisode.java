package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "benhsucacdotbenhtruoc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviousDiseaseEpisode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tien_su", nullable = false) private Long previousDiseaseEpisodeId;
    @Column(name = "ma_ho_so", nullable = false) private Long medicalRecordId;
    @Column(name = "tung_co_dot_tuong_tu") private Boolean previousSimilarEpisodes;
    @Column(name = "tuoi_khoi_phat") private Short onsetAge;
    @Column(name = "so_dot_truoc") private Integer previousEpisodeCount;
    @Column(name = "kieu_dien_bien") private String diseaseCoursePattern;
    @Column(name = "bieu_hien_san_phu") private Boolean whealManifestation;
    @Column(name = "bieu_hien_phu_mach") private Boolean angioedemaManifestation;
    @Column(name = "bieu_hien_khac_ghi_ro") private String otherManifestationDescription;
    @Column(name = "da_tung_nhap_vien", length = 10) private String previouslyHospitalized;
    @Column(name = "so_lan_nhap_vien") private Integer hospitalizationCount;
    @Column(name = "thoi_gian_tu_dot_gan_nhat") private BigDecimal timeSinceLastEpisode;
    @Column(name = "dien_bien_cac_dot_truoc") private String previousEpisodeCourse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho_so", referencedColumnName = "ma_ho_so", insertable = false, updatable = false)
    private ChronicUrticariaRecord chronicUrticariaRecord;
}
