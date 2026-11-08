package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "donthuoc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_don_thuoc", nullable = false) private Long prescriptionId;
    @Column(name = "ma_ho_so", nullable = false) private Long medicalRecordId;
    @Column(name = "ngay_ke_don", nullable = false) private LocalDate prescriptionDate;
    @Column(name = "trang_thai_don", length = 50) private String prescriptionStatus;
    @Column(name = "ghi_chu_dieu_tri") private String treatmentNotes;
    @Column(name = "ngung_thuoc_lan_sau") private Boolean stopMedicationAtNextVisit;
    @Column(name = "thuoc_can_ngung_truoc_tai_kham") private String medicationsToStopBeforeFollowUp;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho_so", referencedColumnName = "ma_ho_so", insertable = false, updatable = false)
    private MedicalRecord medicalRecord;
}
