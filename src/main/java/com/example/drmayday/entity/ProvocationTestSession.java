package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "phiennghiemphapkichthich")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvocationTestSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_phien_nghiem_phap", nullable = false) private Long provocationTestSessionId;
    @Column(name = "ma_phien_kham", nullable = false) private Long medicalVisitId;
    @Column(name = "ngung_khang_histamine") private Boolean stoppedAntihistamines;
    @Column(name = "thoi_gian_ngung_histamine", precision = 8, scale = 2) private BigDecimal histamineDiscontinuationDays;
    @Column(name = "ngung_corticoid") private Boolean stoppedCorticosteroids;
    @Column(name = "thoi_gian_ngung_corticoid", precision = 8, scale = 2) private BigDecimal corticosteroidDiscontinuationDays;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phien_kham", referencedColumnName = "ma_phien_kham", insertable = false, updatable = false)
    private ChronicVisit chronicVisit;
}
