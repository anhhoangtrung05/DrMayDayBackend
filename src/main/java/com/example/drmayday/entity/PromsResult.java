package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "ketquaproms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromsResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_ket_qua_proms", nullable = false) private Long promsResultId;
    @Column(name = "ma_phien_kham_man", nullable = false) private Long chronicVisitId;
    @Column(name = "uct_diem") private Short uctScore;
    @Column(name = "aect_diem") private Short aectScore;
    @Column(name = "act_diem_legacy") private Short actLegacyScore;
    @Column(name = "dlqi_diem") private Short dlqiScore;
    @Column(name = "cu_q2ol_diem", precision = 5, scale = 2) private BigDecimal cuQ2olScore;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phien_kham_man", referencedColumnName = "ma_phien_kham", insertable = false, updatable = false)
    private ChronicVisit chronicVisit;
}
