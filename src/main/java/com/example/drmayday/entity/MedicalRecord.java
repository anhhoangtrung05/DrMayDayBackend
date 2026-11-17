package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "hosobenhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_ho_so", nullable = false) private Long medicalRecordId;
    @Column(name = "ma_benh_nhan", nullable = false) private Long patientId;
    @Column(name = "loai_benh_an", nullable = false) private String medicalRecordType;
    @Column(name = "ma_subtype_cap", insertable = false, updatable = false) private Long acuteSubtypeId;
    @Column(name = "ma_subtype_man", insertable = false, updatable = false) private Long chronicSubtypeId;
    @Column(name = "thoi_diem_kham_lan_dau") private OffsetDateTime firstVisitAt;
    @Column(name = "ngay_bat_dau_dieu_tri_ngoai_tru") private LocalDate outpatientTreatmentStartDate;
    @Column(name = "ngay_ket_thuc_dieu_tri_ngoai_tru") private LocalDate outpatientTreatmentEndDate;
    @Column(name = "chan_doan_tuyen_truoc") private String referringFacilityDiagnosis;
    @Column(name = "chan_doan_ban_dau_phong_kham") private String initialClinicDiagnosis;
    @Column(name = "ket_qua_dieu_tri_bao_hiem") private String insuranceTreatmentOutcome;
    @Column(name = "thoi_gian_khoi_phat_tuan", precision = 12, scale = 2) private BigDecimal onsetDurationWeeks;
    @Column(name = "trang_thai", nullable = false) private String status;
    @Column(name = "tien_su_gia_dinh") private String familyHistory;
    @Column(name = "tien_su_gia_dinh_ghi_ro") private String familyHistoryDescription;
    @Column(name = "da_dieu_tri_truoc_kham") private Boolean treatedBeforeVisit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_benh_nhan", referencedColumnName = "ma_benh_nhan", insertable = false, updatable = false)
    private Patient patient;
}
