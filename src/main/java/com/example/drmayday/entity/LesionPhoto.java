package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "anhchuptonthuong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LesionPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_anh", nullable = false) private Long photoId;
    @Column(name = "ma_tep_dinh_kem", nullable = false) private Long attachmentId;
    @Column(name = "ma_dot_ton_thuong_da") private Long skinLesionEpisodeId;
    @Column(name = "ma_vi_tri_ton_thuong") private Long lesionLocationId;
    @Column(name = "thoi_diem_chup", nullable = false) private OffsetDateTime photographedAt;
    @Column(name = "moc_tien_trien", length = 50) private String progressionMilestone;
    @Column(name = "diem_ngua_nrs") private Short itchNrsScore;
    @Column(name = "diem_dau_nrs") private Short painNrsScore;
    @Column(name = "diem_bong_rat_nrs") private Short burningNrsScore;
    @Column(name = "so_luong_not_san") private Integer whealCount;
    @Column(name = "chi_so_auas_cuc_bo", precision = 5, scale = 2) private BigDecimal localAuasIndex;
    @Column(name = "url_anh", length = 500) private String imageUrl;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_tep_dinh_kem", referencedColumnName = "ma_tep_dinh_kem", insertable = false, updatable = false)
    private Attachment attachment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dot_ton_thuong_da", referencedColumnName = "ma_dot_ton_thuong_da", insertable = false, updatable = false)
    private SkinLesionEpisode skinLesionEpisode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_vi_tri_ton_thuong", referencedColumnName = "ma_vi_tri_ton_thuong", insertable = false, updatable = false)
    private LesionLocation lesionLocation;
}
