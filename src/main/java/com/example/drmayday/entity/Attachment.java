package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "tepdinhkem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tep_dinh_kem", nullable = false) private Long attachmentId;
    @Column(name = "ma_ho_so", nullable = false) private Long medicalRecordId;
    @Column(name = "ma_tai_khoan") private Long accountId;
    @Column(name = "ten_tep_goc", nullable = false, length = 255) private String originalFileName;
    @Column(name = "duong_dan_luu_tru", nullable = false, length = 500) private String storagePath;
    @Column(name = "duong_dan_url", length = 500) private String url;
    @Column(name = "dinh_dang_tep", length = 50) private String fileFormat;
    @Column(name = "loai_phuong_tien", length = 50) private String mediaType;
    @Column(name = "dung_luong_byte") private Long sizeBytes;
    @Column(name = "phan_loai_muc_dich", length = 100) private String purposeCategory;
    @Column(name = "mo_ta_tep") private String fileDescription;
    @Column(name = "diem_do_mo", precision = 6, scale = 2) private BigDecimal blurScore;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho_so", referencedColumnName = "ma_ho_so", insertable = false, updatable = false)
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_tai_khoan", referencedColumnName = "ma_tai_khoan", insertable = false, updatable = false)
    private UserAccount userAccount;
}
