package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ketquacanlamsangman")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChronicClinicalTestResult {
    @Id
    @Column(name = "ma_ket_qua_can_lam_sang", nullable = false) private Long clinicalTestResultId;
    @Column(name = "loai_ket_qua_can_lam_sang", length = 3, insertable = false, updatable = false) private String clinicalTestResultType;
    @Column(name = "test_huyet_thanh_tu_than", length = 12) private String autologousSerumTest;
    @Column(name = "ige_khang_tpo_ket_qua", length = 12) private String antiTpoIgeResult;
    @Column(name = "bat_ket_qua", length = 12) private String basophilActivationTestResult;
    @Column(name = "ige_khang_il24_ket_qua", length = 12) private String antiIl24IgeResult;
    @Column(name = "asst_duong_kinh_mm", precision = 12, scale = 4) private BigDecimal asstDiameterMm;
    @Column(name = "eo", precision = 12, scale = 4) private BigDecimal eosinophils;
    @Column(name = "ba_bach_cau", precision = 12, scale = 4) private BigDecimal basophils;
    @Column(name = "anti_tpo", precision = 12, scale = 4) private BigDecimal antiTpo;
    @Column(name = "tsh", precision = 12, scale = 4) private BigDecimal tsh;
    @Column(name = "ft4", precision = 12, scale = 4) private BigDecimal ft4;
    @Column(name = "ft3", precision = 12, scale = 4) private BigDecimal ft3;
    @Column(name = "mau_lang_2h", precision = 12, scale = 4) private BigDecimal esrAtTwoHours;
    @Column(name = "mau_lang_1h", precision = 12, scale = 4) private BigDecimal esrAtOneHour;
    @Column(name = "ana_hep2", length = 12) private String anaHep2Result;
    @Column(name = "sieu_am_tuyen_giap") private String thyroidUltrasoundResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "ma_ket_qua_can_lam_sang", referencedColumnName = "ma_ket_qua_can_lam_sang", insertable = false, updatable = false),
        @JoinColumn(name = "loai_ket_qua_can_lam_sang", referencedColumnName = "loai_ket_qua_can_lam_sang", insertable = false, updatable = false)
    })
    private ClinicalTestResult clinicalTestResult;
}
