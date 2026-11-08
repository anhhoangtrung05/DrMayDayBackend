package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "dauhieusinhton")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dau_hieu_sinh_ton", nullable = false) private Long vitalSignId;
    @Column(name = "ma_phien_kham", nullable = false) private Long medicalVisitId;
    @Column(name = "ma_bac_si") private Long physicianId;
    @Column(name = "thoi_diem") private OffsetDateTime timestamp;
    @Column(name = "tri_giac") private String consciousness;
    @Column(name = "nhiet_do") private BigDecimal temperature;
    @Column(name = "spo2") private BigDecimal oxygenSaturation;
    @Column(name = "mach") private BigDecimal pulse;
    @Column(name = "huyet_ap_tam_thu") private BigDecimal systolicBloodPressure;
    @Column(name = "huyet_ap_tam_truong") private BigDecimal diastolicBloodPressure;
    @Column(name = "nhip_tho") private BigDecimal respiratoryRate;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phien_kham", referencedColumnName = "ma_phien_kham", insertable = false, updatable = false)
    private MedicalVisit medicalVisit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_bac_si", referencedColumnName = "ma_nhan_vien", insertable = false, updatable = false)
    private Employee employee;
}
