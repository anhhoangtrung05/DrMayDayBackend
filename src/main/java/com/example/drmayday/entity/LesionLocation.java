package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "vitritonthuong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LesionLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_vi_tri_ton_thuong", nullable = false) private Long lesionLocationId;
    @Column(name = "ma_dot_ton_thuong_da", nullable = false) private Long skinLesionEpisodeId;
    @Column(name = "vi_tri_co_the", length = 255) private String bodyLocation;
    @Column(name = "thoi_diem_xuat_hien") private OffsetDateTime onsetAt;
    @Column(name = "ten_thuoc_lien_quan", length = 255) private String relatedMedicationName;
    @Column(name = "thoi_gian_dung_thuoc_truoc_khi_xuat_hien", length = 100) private String medicationUseDurationBeforeOnset;
    @Column(name = "hoan_canh_xuat_hien", length = 255) private String onsetContext;
    @Column(name = "hoan_canh_xuat_hien_khac") private String otherOnsetContext;
    @Column(name = "don_vi_thoi_gian", length = 50) private String durationUnit;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dot_ton_thuong_da", referencedColumnName = "ma_dot_ton_thuong_da", insertable = false, updatable = false)
    private SkinLesionEpisode skinLesionEpisode;
}
