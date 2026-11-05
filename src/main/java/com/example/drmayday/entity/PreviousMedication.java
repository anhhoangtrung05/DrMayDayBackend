package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "chitietthuocdieutritruocdo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviousMedication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_thuoc_dieu_tri_truoc_do", nullable = false) private Long previousMedicationId;
    @Column(name = "ten_thuoc", nullable = false, length = 255) private String medicationName;
    @Column(name = "lieu_dung", length = 100) private String dosage;
    @Column(name = "thoi_gian_dung", length = 100) private String usageDuration;
    @Column(name = "tuan_thu") private String adherence;
    @Column(name = "dap_ung") private String response;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;
}
