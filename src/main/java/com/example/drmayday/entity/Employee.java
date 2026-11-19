package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nhanvien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {
    @Id
    @Column(name = "ma_nhan_vien")
    private Long employeeId;

    @Column(name = "ma_chi_nhanh", nullable = false)
    private Long branchId;

    @Column(name = "ma_khoa")
    private Long departmentId;

    @Column(name = "ma_phong_kham")
    private Long roomId;
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Uses the associated person's ID as this entity's primary key.
    @JoinColumn(name = "ma_nhan_vien", referencedColumnName = "ma_nguoi")
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_chi_nhanh", referencedColumnName = "ma_chi_nhanh", insertable = false, updatable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_khoa", referencedColumnName = "ma_khoa", insertable = false, updatable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phong_kham", referencedColumnName = "ma_phong_kham", insertable = false, updatable = false)
    private ClinicRoom room;
}
