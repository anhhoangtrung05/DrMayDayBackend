package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "dottonthuongda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkinLesionEpisode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dot_ton_thuong_da", nullable = false) private Long skinLesionEpisodeId;
    @Column(name = "ma_benh_nhan", nullable = false) private Long patientId;
    @Column(name = "ma_ho_so_man") private Long chronicMedicalRecordId;
    @Column(name = "ten_dot_ton_thuong_da", length = 255) private String episodeName;
    @Column(name = "thoi_diem_xuat_hien") private OffsetDateTime onsetAt;
    @Column(name = "hoan_canh_xuat_hien", length = 255) private String onsetContext;
    @Column(name = "hoan_canh_xuat_hien_khac") private String otherOnsetContext;
    @Column(name = "ten_thuoc_dung_gan_thoi_diem_xuat_hien", length = 255) private String medicationUsedNearOnset;
    @Column(name = "thoi_gian_dung_thuoc_truoc_khi_xuat_hien", length = 100) private String medicationUseDurationBeforeOnset;
    @Column(name = "don_vi_thoi_gian", length = 50) private String durationUnit;
    @Column(name = "chi_tiet_dot_ton_thuong") private String episodeDetails;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_benh_nhan", referencedColumnName = "ma_benh_nhan", insertable = false, updatable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho_so_man", referencedColumnName = "ma_ho_so", insertable = false, updatable = false)
    private ChronicUrticariaRecord chronicUrticariaRecord;
}
