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
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Uses the associated person's ID as this entity's primary key.
    @JoinColumn(name = "ma_nhan_vien", referencedColumnName = "ma_nguoi")
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_chi_nhanh", referencedColumnName = "ma_chi_nhanh", insertable = false, updatable = false)
    private Branch branch;
}
