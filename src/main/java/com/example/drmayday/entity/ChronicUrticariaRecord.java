package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "benhanmaydayman")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChronicUrticariaRecord {
    @Id
    @Column(name = "ma_ho_so", nullable = false) private Long medicalRecordId;
    @Column(name = "loai_benh_an", insertable = false, updatable = false) private String medicalRecordType;
    @Column(name = "chan_doan_chinh") private String primaryDiagnosis;
    @Column(name = "phan_nhom_csu") private String csuSubtype;
    @Column(name = "chung_da_ve_noi") private Boolean dermographism;
    @Column(name = "may_day_cholinergic") private Boolean cholinergicUrticaria;
    @Column(name = "may_day_do_lanh") private Boolean coldUrticaria;
    @Column(name = "may_day_adrenergic") private Boolean adrenergicUrticaria;
    @Column(name = "may_day_do_nuoc") private Boolean aquagenicUrticaria;
    @Column(name = "may_day_do_anh_sang") private Boolean solarUrticaria;
    @Column(name = "may_day_cam_ung_khac") private Boolean otherInducibleUrticaria;
    @Column(name = "may_day_cam_ung_khac_ghi_ro") private String otherInducibleUrticariaDescription;
    @Column(name = "bieu_hien_qua_trinh_benh") private String diseaseCourseManifestation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "ma_ho_so", referencedColumnName = "ma_ho_so", insertable = false, updatable = false),
        @JoinColumn(name = "loai_benh_an", referencedColumnName = "loai_benh_an", insertable = false, updatable = false)
    })
    private MedicalRecord medicalRecord;
}
