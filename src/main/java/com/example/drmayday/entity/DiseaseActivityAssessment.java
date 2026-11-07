package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "danhgiamucdohoatdongbenh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiseaseActivityAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_danh_gia_hoat_dong", nullable = false) private Long diseaseActivityAssessmentId;
    @Column(name = "ma_phien_kham_man", nullable = false) private Long chronicVisitId;
    @Column(name = "du_du_lieu_7_ngay") private Boolean hasSevenDaysData;
    @Column(name = "ma_nguoi_danh_gia") private Long assessorId;
    @Column(name = "thoi_diem_danh_gia", nullable = false) private OffsetDateTime assessedAt;
    @Column(name = "hss7_san_phu_khong_dung_thuoc") private Short hss7WhealWithoutMedication;
    @Column(name = "hss7_san_phu_dung_thuoc_deu") private Short hss7WhealWithRegularMedication;
    @Column(name = "hss7_san_phu_dung_thuoc_khong_deu") private Short hss7WhealWithIrregularMedication;
    @Column(name = "iss7_ngua_khong_dung_thuoc") private Short iss7ItchWithoutMedication;
    @Column(name = "iss7_ngua_dung_thuoc_deu") private Short iss7ItchWithRegularMedication;
    @Column(name = "iss7_ngua_dung_thuoc_khong_deu") private Short iss7ItchWithIrregularMedication;
    @Column(name = "uas7_tong_khong_dung_thuoc") private Short uas7TotalWithoutMedication;
    @Column(name = "uas7_tong_dung_thuoc_deu") private Short uas7TotalWithRegularMedication;
    @Column(name = "uas7_tong_dung_thuoc_khong_deu") private Short uas7TotalWithIrregularMedication;
    @Column(name = "ghi_chu") private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phien_kham_man", referencedColumnName = "ma_phien_kham", insertable = false, updatable = false)
    private ChronicVisit chronicVisit;
}
