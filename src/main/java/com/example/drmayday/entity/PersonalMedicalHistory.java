package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tiensubanthan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalMedicalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tien_su_ban_than", nullable = false) private Long personalMedicalHistoryId;
    @Column(name = "ma_ho_so", nullable = false) private Long medicalRecordId;
    @Column(name = "tien_su_co_dia", length = 10) private String atopicHistory;
    @Column(name = "co_dia_viem_da_co_dia") private Boolean atopicDermatitis;
    @Column(name = "co_dia_hen") private Boolean asthma;
    @Column(name = "co_dia_viem_mui_di_ung") private Boolean allergicRhinitis;
    @Column(name = "co_dia_khac") private Boolean otherAtopicConditions;
    @Column(name = "co_dia_khac_ghi_ro") private String otherAtopicConditionsDescription;
    @Column(name = "tien_su_tuyen_giap") private Boolean thyroidHistory;
    @Column(name = "tien_su_tuyen_giap_ghi_ro") private String thyroidHistoryDescription;
    @Column(name = "tien_su_tu_mien") private Boolean autoimmuneHistory;
    @Column(name = "tien_su_tu_mien_ghi_ro") private String autoimmuneHistoryDescription;
    @Column(name = "tien_su_di_ung", length = 10) private String allergyHistory;
    @Column(name = "di_ung_thuoc") private Boolean medicationAllergy;
    @Column(name = "di_ung_thuoc_ghi_ro") private String medicationAllergyDescription;
    @Column(name = "di_ung_thuc_an") private Boolean foodAllergy;
    @Column(name = "di_ung_thuc_an_ghi_ro") private String foodAllergyDescription;
    @Column(name = "di_ung_di_nguyen_khac") private Boolean otherAllergy;
    @Column(name = "di_ung_di_nguyen_khac_ghi_ro") private String otherAllergyDescription;
    @Column(name = "tien_su_phan_ve") private Boolean anaphylaxisHistory;
    @Column(name = "tien_su_phan_ve_ghi_ro") private String anaphylaxisHistoryDescription;
    @Column(name = "tien_su_viem_nhiem_man") private Boolean chronicInfectionHistory;
    @Column(name = "tien_su_viem_nhiem_man_ghi_ro") private String chronicInfectionHistoryDescription;
    @Column(name = "tien_su_benh_khac") private Boolean otherMedicalHistory;
    @Column(name = "tien_su_benh_khac_ghi_ro") private String otherMedicalHistoryDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho_so", referencedColumnName = "ma_ho_so", insertable = false, updatable = false)
    private MedicalRecord medicalRecord;
}
