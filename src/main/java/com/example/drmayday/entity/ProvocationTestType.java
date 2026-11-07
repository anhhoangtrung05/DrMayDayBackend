package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "loainghiemphap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvocationTestType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nghiem_phap", nullable = false) private Long provocationTestId;
    @Column(name = "ten_nghiem_phap", nullable = false, length = 150) private String testName;
    @Column(name = "phuong_phap_thuc_hien") private String implementationMethod;
    @Column(name = "thoi_gian_chuan", precision = 10, scale = 2) private BigDecimal standardDuration;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;
}
