package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "ketquacanlamsang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalTestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_ket_qua_can_lam_sang", nullable = false) private Long clinicalTestResultId;
    @Column(name = "ma_phien_kham", nullable = false) private Long medicalVisitId;
    @Column(name = "loai_ket_qua_can_lam_sang", nullable = false, length = 3) private String clinicalTestResultType;
    @Column(name = "wbc", precision = 12, scale = 4) private BigDecimal wbc;
    @Column(name = "crp", precision = 12, scale = 4) private BigDecimal crp;
    @Column(name = "ige_toan_phan", precision = 12, scale = 4) private BigDecimal totalIge;
    @Column(name = "can_lam_sang_khac_ghi_chu") private String otherClinicalTestNotes;
    @Column(name = "xet_nghiem_khac") private String otherLaboratoryTest;
    @Column(name = "thoi_diem_thuc_hien") private OffsetDateTime performedAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "ma_phien_kham", referencedColumnName = "ma_phien_kham", insertable = false, updatable = false),
        @JoinColumn(name = "loai_ket_qua_can_lam_sang", referencedColumnName = "loai_benh_an", insertable = false, updatable = false)
    })
    private MedicalVisit medicalVisit;
}
