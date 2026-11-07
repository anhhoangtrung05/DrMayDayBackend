package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "nhatkyuashangngay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyUasLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nhat_ky_uas", nullable = false) private Long dailyUasLogId;
    @Column(name = "ma_chu_ky") private Long trackingCycleId;
    @Column(name = "ma_ho_so_man") private Long chronicMedicalRecordId;
    @Column(name = "ma_benh_nhan", nullable = false) private Long patientId;
    @Column(name = "ngay_cham_diem", nullable = false) private LocalDate scoreDate;
    @Column(name = "ngay_thu_may_trong_chu_ky") private Short cycleDay;
    @Column(name = "diem_san_phu_hss", nullable = false) private Short whealScoreHss;
    @Column(name = "diem_ngua_iss", nullable = false) private Short itchScoreIss;
    @Column(name = "diem_uas_ngay", insertable = false, updatable = false) private Short dailyUasScore;
    @Column(name = "trang_thai_dung_thuoc", length = 50) private String medicationAdherenceStatus;
    @Column(name = "trang_thai_ngay", length = 50) private String dayStatus;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_chu_ky", referencedColumnName = "ma_chu_ky", insertable = false, updatable = false)
    private Uas7TrackingCycle uas7TrackingCycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho_so_man", referencedColumnName = "ma_ho_so", insertable = false, updatable = false)
    private ChronicUrticariaRecord chronicUrticariaRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_benh_nhan", referencedColumnName = "ma_benh_nhan", insertable = false, updatable = false)
    private Patient patient;
}
