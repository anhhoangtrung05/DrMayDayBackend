package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "nhatkyaashangngay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyAasLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nhat_ky_aas", nullable = false) private Long dailyAasLogId;
    @Column(name = "ma_benh_nhan", nullable = false) private Long patientId;
    @Column(name = "ma_ho_so_man") private Long chronicMedicalRecordId;
    @Column(name = "ngay_theo_doi", nullable = false) private LocalDate trackingDate;
    @Column(name = "co_dot_sung_phu", nullable = false) private Boolean hasAngioedemaEpisode;
    @Column(name = "thoi_diem_xuat_hien", length = 50) private String onsetAt;
    @Column(name = "muc_kho_chiu") private Short discomfortLevel;
    @Column(name = "han_che_hoat_dong") private Short activityLimitation;
    @Column(name = "anh_huong_ngoai_hinh") private Short appearanceImpact;
    @Column(name = "do_nang_tong_the") private Short overallSeverity;
    @Column(name = "diem_aas_ngay", insertable = false, updatable = false) private Short dailyAasScore;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_benh_nhan", referencedColumnName = "ma_benh_nhan", insertable = false, updatable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho_so_man", referencedColumnName = "ma_ho_so", insertable = false, updatable = false)
    private ChronicUrticariaRecord chronicUrticariaRecord;
}
