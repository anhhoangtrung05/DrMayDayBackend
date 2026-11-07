package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "chitiettonthuongsanphu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhealLesionDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chi_tiet_ton_thuong", nullable = false) private Long whealLesionDetailId;
    @Column(name = "ma_phien_kham", nullable = false) private Long medicalVisitId;
    @Column(name = "dang_ton_thuong") private String lesionType;
    @Column(name = "kich_thuoc") private BigDecimal size;
    @Column(name = "thoi_gian_ton_tai") private BigDecimal lesionDuration;
    @Column(name = "thoi_diem") private OffsetDateTime timestamp;
    @Column(name = "tinh_trang_dung_thuoc") private String medicationStatus;
    @Column(name = "ma_dac_diem_san_phu") private Long whealFeatureId;
    @Column(name = "so_luong") private Integer quantity;
    @Column(name = "vi_tri") private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phien_kham", referencedColumnName = "ma_phien_kham", insertable = false, updatable = false)
    private ChronicVisit chronicVisit;
}
