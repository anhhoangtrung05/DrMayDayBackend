package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "phongkham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_phong_kham", nullable = false) private Long clinicRoomId;
    @Column(name = "ma_chi_nhanh", nullable = false) private Long branchId;
    @Column(name = "ten_phong_kham", nullable = false, length = 150) private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_chi_nhanh", referencedColumnName = "ma_chi_nhanh", insertable = false, updatable = false)
    private Branch branch;
}
