package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "thuoc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {
    @Id
    @Column(name = "ma_thuoc", nullable = false, length = 50) private String medicationId;
    @Column(name = "ten_thuoc", nullable = false, length = 255) private String medicationName;
    @Column(name = "hoat_chat", length = 255) private String activeIngredient;
    @Column(name = "nhom_dieu_tri", length = 100) private String treatmentGroup;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;
}
