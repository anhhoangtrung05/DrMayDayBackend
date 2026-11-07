package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "chitiettonthuongcaptinh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcuteLesionDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chi_tiet_ton_thuong_cap_tinh", nullable = false) private Long acuteLesionDetailId;
    @Column(name = "ma_phien_kham", nullable = false) private Long medicalVisitId;
    @Column(name = "dang_ton_thuong") private String lesionType;
    @Column(name = "dang_ton_thuong_khac") private String otherLesionType;
    @Column(name = "kich_thuoc_max_mm") private BigDecimal maximumSizeMm;
    @Column(name = "kich_thuoc_min_mm") private BigDecimal minimumSizeMm;
    @Column(name = "kich_thuoc_dong_deu_mm") private BigDecimal uniformSizeMm;
    @Column(name = "loai_kich_thuoc") private String sizeType;
    @Column(name = "thoi_gian_ton_tai_tb") private BigDecimal averageLesionDuration;
    @Column(name = "dien_bien") private String lesionCourse;
    @Column(name = "so_luong_san_phu") private Integer whealCount;
    @Column(name = "phu_mach_ben") private Boolean angioedemaPresent;
    @Column(name = "vi_tri") private String location;
    @Column(name = "vi_tri_dac_biet_ghi_ro") private String specialLocationDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phien_kham", referencedColumnName = "ma_phien_kham", insertable = false, updatable = false)
    private AcuteVisit acuteVisit;
}
