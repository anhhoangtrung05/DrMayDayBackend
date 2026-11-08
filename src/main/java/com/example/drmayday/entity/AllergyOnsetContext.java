package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "hoancanhkhoiphatdiung")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllergyOnsetContext {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_hoan_canh_khoi_phat", nullable = false) private Long allergyOnsetContextId;
    @Column(name = "ma_ho_so", nullable = false) private Long medicalRecordId;
    @Column(name = "thuc_an_nghi_ngo") private String suspectedFoodTrigger;
    @Column(name = "thuc_an_thoi_gian") private BigDecimal foodTriggerDuration;
    @Column(name = "thuc_an_tung_bi") private Boolean previousFoodReaction;
    @Column(name = "thuc_an_loai_tien_su") private String foodHistoryType;
    @Column(name = "thuc_an_tien_su_ghi_ro") private String foodHistoryDescription;
    @Column(name = "thuoc_nghi_ngo") private String suspectedMedicationTrigger;
    @Column(name = "thuoc_thoi_gian") private BigDecimal medicationTriggerDuration;
    @Column(name = "thuoc_tung_bi") private Boolean previousMedicationReaction;
    @Column(name = "thuoc_loai_tien_su") private String medicationHistoryType;
    @Column(name = "thuoc_tien_su_ghi_ro") private String medicationHistoryDescription;
    @Column(name = "con_trung_nghi_ngo") private String suspectedInsectTrigger;
    @Column(name = "con_trung_thoi_gian") private BigDecimal insectTriggerDuration;
    @Column(name = "con_trung_tung_bi") private Boolean previousInsectReaction;
    @Column(name = "con_trung_loai_tien_su") private String insectHistoryType;
    @Column(name = "con_trung_tien_su_ghi_ro") private String insectHistoryDescription;
    @Column(name = "khac_nghi_ngo") private String suspectedOtherTrigger;
    @Column(name = "khac_thoi_gian") private BigDecimal otherDuration;
    @Column(name = "khac_tung_bi") private Boolean previousOtherReaction;
    @Column(name = "khac_loai_tien_su") private String otherHistoryType;
    @Column(name = "khac_tien_su_ghi_ro") private String otherHistoryDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho_so", referencedColumnName = "ma_ho_so", insertable = false, updatable = false)
    private MedicalRecord medicalRecord;
}
