package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "khoa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_khoa")
    private Long departmentId;

    @Column(name = "ma_chi_nhanh", nullable = false)
    private Long branchId;

    @Column(name = "ten_khoa", nullable = false, length = 150)
    private String name;
}
