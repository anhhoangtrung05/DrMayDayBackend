package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tinhtrangnhiemtrung")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfectionStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tinh_trang_nhiem_trung", nullable = false) private Long infectionStatusId;
    @Column(name = "ma_ho_so", nullable = false) private Long medicalRecordId;
    @Column(name = "ho_hap_ho") private Boolean respiratoryCough;
    @Column(name = "ho_hap_dau_hong") private Boolean respiratorySoreThroat;
    @Column(name = "ho_hap_hat_hoi") private Boolean respiratorySneezing;
    @Column(name = "ho_hap_so_mui") private Boolean respiratoryRunnyNose;
    @Column(name = "ho_hap_thoi_gian") private BigDecimal respiratoryDuration;
    @Column(name = "ho_hap_khoang_cach") private BigDecimal respiratoryInterval;
    @Column(name = "tiet_nieu_tieu_rat") private Boolean urinaryPainfulUrination;
    @Column(name = "tiet_nieu_tieu_buot") private Boolean urinaryBurningUrination;
    @Column(name = "tiet_nieu_thoi_gian") private BigDecimal urinaryDuration;
    @Column(name = "tiet_nieu_khoang_cach") private BigDecimal urinaryInterval;
    @Column(name = "da_mun") private Boolean skinAcne;
    @Column(name = "da_nhot") private Boolean skinBoils;
    @Column(name = "da_thoi_gian") private BigDecimal skinDuration;
    @Column(name = "da_khoang_cach") private BigDecimal skinInterval;
    @Column(name = "khac_ghi_ro") private String otherDescription;
    @Column(name = "khac_thoi_gian") private BigDecimal otherDuration;
    @Column(name = "khac_khoang_cach") private BigDecimal otherInterval;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho_so", referencedColumnName = "ma_ho_so", insertable = false, updatable = false)
    private AcuteUrticariaRecord acuteUrticariaRecord;
}
