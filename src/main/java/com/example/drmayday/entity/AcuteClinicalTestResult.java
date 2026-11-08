package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ketquacanlamsangcap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcuteClinicalTestResult {
    @Id
    @Column(name = "ma_ket_qua_can_lam_sang", nullable = false) private Long clinicalTestResultId;
    @Column(name = "loai_ket_qua_can_lam_sang", length = 3, insertable = false, updatable = false) private String clinicalTestResultType;
    @Column(name = "lym", precision = 12, scale = 4) private BigDecimal lymphocytes;
    @Column(name = "neu", precision = 12, scale = 4) private BigDecimal neutrophils;
    @Column(name = "plt", precision = 12, scale = 4) private BigDecimal platelets;
    @Column(name = "tryptase_cap", precision = 12, scale = 4) private BigDecimal acuteTryptase;
    @Column(name = "tryptase_nen", precision = 12, scale = 4) private BigDecimal baselineTryptase;
    @Column(name = "nlr", insertable = false, updatable = false) private BigDecimal neutrophilToLymphocyteRatio;
    @Column(name = "plr", insertable = false, updatable = false) private BigDecimal plateletToLymphocyteRatio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "ma_ket_qua_can_lam_sang", referencedColumnName = "ma_ket_qua_can_lam_sang", insertable = false, updatable = false),
        @JoinColumn(name = "loai_ket_qua_can_lam_sang", referencedColumnName = "loai_ket_qua_can_lam_sang", insertable = false, updatable = false)
    })
    private ClinicalTestResult clinicalTestResult;
}
