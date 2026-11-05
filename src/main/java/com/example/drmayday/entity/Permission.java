package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name = "quyenhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_quyen_han", nullable = false) private Long permissionId;
    @Column(name = "ten_quyen_han", nullable = false, length = 100) private String name;
    @Column(name = "nhom_quyen", length = 100) private String permissionGroup;
    @Column(name = "mo_ta") private String description;
    @Column(name = "thoi_diem_tao", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "thoi_diem_cap_nhat", nullable = false) private OffsetDateTime updatedAt;
}
