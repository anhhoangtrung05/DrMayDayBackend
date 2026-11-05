package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "phienkham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalVisit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_phien_kham", nullable = false) private Long medicalVisitId;
    @Column(name = "ma_ho_so", nullable = false) private Long medicalRecordId;
    @Column(name = "ma_bac_si", nullable = false) private Long physicianId;
    @Column(name = "ma_phong_kham") private Long clinicRoomId;
    @Column(name = "loai_benh_an", nullable = false) private String medicalRecordType;
    @Column(name = "ma_subtype_cap", insertable = false, updatable = false) private Long acuteSubtypeId;
    @Column(name = "ma_subtype_man", insertable = false, updatable = false) private Long chronicSubtypeId;
    @Column(name = "ngay_kham", nullable = false) private LocalDate visitDate;
    @Column(name = "lan_kham_thu", nullable = false) private Integer visitNumber;
    @Column(name = "loai_phien_kham") private String visitType;
    @Column(name = "luu_huyet_thanh") private Boolean serumSampleStored;
    @Column(name = "sot") private Boolean fever;
    @Column(name = "sot_nhiet_do_c", precision = 4, scale = 1) private BigDecimal feverTemperatureCelsius;
    @Column(name = "ngay_hen_tai_kham") private LocalDate followUpDate;
    @Column(name = "tom_tat_buoi_kham") private String visitSummary;
    @Column(name = "huong_xu_tri_lan_sau") private String nextVisitManagement;
    @Column(name = "dan_do") private String instructions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "ma_ho_so", referencedColumnName = "ma_ho_so", insertable = false, updatable = false),
        @JoinColumn(name = "loai_benh_an", referencedColumnName = "loai_benh_an", insertable = false, updatable = false)
    })
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_bac_si", referencedColumnName = "ma_nhan_vien", insertable = false, updatable = false)
    private Employee employee;
}
