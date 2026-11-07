package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "chitietketquanghiemphap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvocationTestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chi_tiet_ket_qua_nghiem_phap", nullable = false) private Long provocationTestResultId;
    @Column(name = "ma_phien_nghiem_phap", nullable = false) private Long provocationTestSessionId;
    @Column(name = "loai_nghiem_phap", nullable = false) private Long provocationTestTypeId;
    @Column(name = "ket_qua", length = 12) private String testResult;
    @Column(name = "nguong_gia_tri_so", precision = 12, scale = 4) private BigDecimal numericThreshold;
    @Column(name = "nguong_gia_tri_chu") private String textThreshold;
    @Column(name = "don_vi_do", length = 30) private String measurementUnit;
    @Column(name = "diem_ngua_nrs") private Short itchNrsScore;
    @Column(name = "diem_bong_rat_nrs") private Short burningNrsScore;
    @Column(name = "diem_dau_nrs") private Short painNrsScore;
    @Column(name = "can_nguyen_khac_ten") private String otherCauseName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phien_nghiem_phap", referencedColumnName = "ma_phien_nghiem_phap", insertable = false, updatable = false)
    private ProvocationTestSession provocationTestSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loai_nghiem_phap", referencedColumnName = "ma_nghiem_phap", insertable = false, updatable = false)
    private ProvocationTestType provocationTestType;
}
