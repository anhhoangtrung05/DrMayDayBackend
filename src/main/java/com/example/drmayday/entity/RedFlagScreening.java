package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "sanglocbaodongdo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedFlagScreening {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_luot_sang_loc", nullable = false) private Long screeningId;
    @Column(name = "ma_phien_ung_dung", nullable = false) private UUID applicationSessionId;
    @Column(name = "ma_benh_nhan") private Long patientId;
    @Column(name = "phien_ban_bo_cau_hoi", nullable = false, length = 30) private String questionnaireVersion;
    @Column(name = "co_kho_tho_kho_khe_nghen_hong", nullable = false) private Boolean hasBreathingDifficultyOrWheezingOrThroatTightness;
    @Column(name = "co_chong_mat_tuc_nguc_sap_ngat", nullable = false) private Boolean hasDizzinessOrChestTightnessOrFainting;
    @Column(name = "co_sung_moi_luoi_co_hong", nullable = false) private Boolean hasLipTongueOrThroatSwelling;
    @Column(name = "co_dau_bung_non_lien_tuc", nullable = false) private Boolean hasAbdominalPainOrPersistentVomiting;
    @Column(name = "co_nguy_co_phan_ve", insertable = false, updatable = false) private Boolean hasAnaphylaxisRisk;
    @Column(name = "ket_qua_dieu_huong", length = 30, insertable = false, updatable = false) private String routingResult;
    @Column(name = "xac_nhan_muon_goi_cap_cuu") private Boolean confirmedEmergencyCall;
    @Column(name = "da_nhan_nut_goi_cap_cuu", nullable = false) private Boolean emergencyCallButtonPressed;
    @Column(name = "so_dien_thoai_cap_cuu", length = 20) private String emergencyPhoneNumber;
    @Column(name = "thoi_diem_nhan_nut_goi") private OffsetDateTime emergencyCallPressedAt;
    @Column(name = "thoi_diem_bat_dau", nullable = false) private OffsetDateTime startedAt;
    @Column(name = "thoi_diem_hoan_tat", nullable = false) private OffsetDateTime completedAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_benh_nhan", referencedColumnName = "ma_benh_nhan", insertable = false, updatable = false)
    private Patient patient;
}
