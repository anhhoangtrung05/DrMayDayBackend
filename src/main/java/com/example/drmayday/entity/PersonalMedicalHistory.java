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
    // Khớp đúng bảng tiensubanthan trong sql/final_restructured.sql: mỗi tiền sử là một cờ BOOLEAN kèm ghi chú
    @Column(name = "tien_su_co_dia") private Boolean atopicHistory;
    @Column(name = "tien_su_co_dia_ghi_ro") private String atopicHistoryDescription;
    @Column(name = "tien_su_tuyen_giap") private Boolean thyroidHistory;
    @Column(name = "tien_su_tuyen_giap_ghi_ro") private String thyroidHistoryDescription;
    @Column(name = "tien_su_tu_mien") private Boolean autoimmuneHistory;
    @Column(name = "tien_su_tu_mien_ghi_ro") private String autoimmuneHistoryDescription;
    @Column(name = "tien_su_di_ung") private Boolean allergyHistory;
    @Column(name = "tien_su_di_ung_ghi_ro") private String allergyHistoryDescription;
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
