package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "chitietdonthuoc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chi_tiet_don_thuoc", nullable = false) private Long prescriptionItemId;
    @Column(name = "ma_don_thuoc", nullable = false) private Long prescriptionId;
    @Column(name = "ma_thuoc", length = 50) private String medicationId;
    @Column(name = "ten_thuoc_bo_sung", length = 255) private String additionalMedicationName;
    @Column(name = "lieu_dung", nullable = false, length = 100) private String dosage;
    @Column(name = "thoi_gian_dung", nullable = false, length = 100) private String usageDuration;
    @Column(name = "ma_ke_hoach_dieu_tri", length = 50) private String treatmentPlanId;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_don_thuoc", referencedColumnName = "ma_don_thuoc", insertable = false, updatable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_thuoc", referencedColumnName = "ma_thuoc", insertable = false, updatable = false)
    private Medication medication;
}
