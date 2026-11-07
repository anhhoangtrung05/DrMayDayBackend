package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "chukytheodoiuas7")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Uas7TrackingCycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chu_ky", nullable = false) private Long trackingCycleId;
    @Column(name = "ma_benh_nhan", nullable = false) private Long patientId;
    @Column(name = "ngay_bat_dau", nullable = false) private LocalDate startDate;
    @Column(name = "ngay_ket_thuc", nullable = false) private LocalDate endDate;
    @Column(name = "trang_thai_chu_ky", length = 50) private String cycleStatus;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_benh_nhan", referencedColumnName = "ma_benh_nhan", insertable = false, updatable = false)
    private Patient patient;
}
