package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "phienkhamcap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcuteVisit {
    @Id
    @Column(name = "ma_phien_kham", nullable = false) private Long medicalVisitId;
    @Column(name = "loai_benh_an", insertable = false, updatable = false) private String medicalRecordType;
    @Column(name = "trieu_chung_da") private String skinSymptoms;
    @Column(name = "trieu_chung_ho_hap") private String respiratorySymptoms;
    @Column(name = "so_voi_thuong_ngay_ho_hap") private String respiratorySymptomsComparedToUsual;
    @Column(name = "trieu_chung_tuan_hoan") private String cardiovascularSymptoms;
    @Column(name = "so_voi_thuong_ngay_tuan_hoan") private String cardiovascularSymptomsComparedToUsual;
    @Column(name = "trieu_chung_tieu_hoa") private String gastrointestinalSymptoms;
    @Column(name = "so_voi_thuong_ngay_tieu_hoa") private String gastrointestinalSymptomsComparedToUsual;
    @Column(name = "co_san_phu") private Boolean hasWheals;
    @Column(name = "co_anh_san_phu") private Boolean hasWhealPhotos;
    @Column(name = "so_lan_non") private Integer vomitingEpisodes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "ma_phien_kham", referencedColumnName = "ma_phien_kham", insertable = false, updatable = false),
        @JoinColumn(name = "loai_benh_an", referencedColumnName = "loai_benh_an", insertable = false, updatable = false)
    })
    private MedicalVisit medicalVisit;
}
