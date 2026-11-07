package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "chidanngungthuoc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationDiscontinuation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chi_dan_ngung", nullable = false) private Long discontinuationId;
    @Column(name = "ma_don_thuoc", nullable = false) private Long prescriptionId;
    @Column(name = "thuoc_can_ngung", nullable = false, length = 255) private String medicationToStop;
    @Column(name = "ngung_tu_ngay") private LocalDate stopDate;
    @Column(name = "thoi_gian_ngung_ngay") private Integer discontinuationDays;
    @Column(name = "muc_dich", length = 100) private String purpose;
    @Column(name = "muc_dich_khac_ghi_ro", length = 255) private String otherPurposeDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_don_thuoc", referencedColumnName = "ma_don_thuoc", insertable = false, updatable = false)
    private Prescription prescription;
}
