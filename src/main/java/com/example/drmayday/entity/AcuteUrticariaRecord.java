package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "benhanmaydaycap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcuteUrticariaRecord {
    @Id
    @Column(name = "ma_ho_so", nullable = false) private Long medicalRecordId;
    @Column(name = "loai_benh_an", insertable = false, updatable = false) private String medicalRecordType;
    @Column(name = "nhap_vien") private Boolean hospitalized;
    @Column(name = "cd_soc_phan_ve") private Boolean diagnosedAnaphylacticShock;
    @Column(name = "cd_phan_ve") private Boolean diagnosedAnaphylaxis;
    @Column(name = "cd_may_day_cap_nang") private Boolean diagnosedSevereAcuteUrticaria;
    @Column(name = "cd_may_day_cap_thong_thuong") private Boolean diagnosedTypicalAcuteUrticaria;
    @Column(name = "tien_su_benh_nen_da") private String skinComorbidityHistory;
    @Column(name = "tien_su_benh_nen_ho_hap") private String respiratoryComorbidityHistory;
    @Column(name = "tien_su_benh_nen_tuan_hoan") private String circulatoryComorbidityHistory;
    @Column(name = "tien_su_benh_tim_mach_ghi_ro") private String cardiovascularHistoryDescription;
    @Column(name = "tien_su_benh_tieu_hoa_man_tinh_ghi_ro") private String chronicGastrointestinalHistoryDescription;
    @Column(name = "tien_su_benh_nen_tieu_hoa") private String gastrointestinalComorbidityHistory;
    @Column(name = "nhom_yeu_to_nghi_ngo") private String suspectedTriggerGroup;
    @Column(name = "co_nghi_ngo_dot_nay") private Boolean suspectedTriggerInCurrentEpisode;
    @Column(name = "ten_yeu_to_nghi_ngo") private String suspectedTriggerName;
    @Column(name = "loai_tien_su") private String historyType;
    @Column(name = "da_tung_phan_ung_truoc_day") private Boolean previousReaction;
    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "thoi_gian_tiep_xuc_den_khoi_phat") private Duration exposureToOnsetDuration;
    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "thoi_gian_keo_dai_tb") private Duration averageDuration;
    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "dot_gan_nhat_cach") private Duration timeSinceLastEpisode;
    @Column(name = "tien_su_phan_ve_ghi_ro") private String anaphylaxisHistoryDescription;
    @Column(name = "tien_su_di_ung_ghi_ro") private String allergyHistoryDescription;
    @Column(name = "da_dieu_tri_benh_dot_nay") private Boolean treatedCurrentEpisode;
    @Column(name = "tuoi_lan_dau_bi_benh") private Integer ageAtFirstOnset;
    @Column(name = "so_lan_may_day_cap_truoc") private Integer previousAcuteUrticariaCount;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "benh_phu") private String comorbidities;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "ma_ho_so", referencedColumnName = "ma_ho_so", insertable = false, updatable = false),
        @JoinColumn(name = "loai_benh_an", referencedColumnName = "loai_benh_an", insertable = false, updatable = false)
    })
    private MedicalRecord medicalRecord;
}
