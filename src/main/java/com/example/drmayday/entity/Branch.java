package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chinhanh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chi_nhanh")
    private Long branchId;

    @Column(name = "ten_chi_nhanh", nullable = false, length = 150)
    private String name;

    @Column(name = "dia_chi", nullable = false)
    private String address;

    @Column(name = "so_dien_thoai", length = 20)
    private String phoneNumber;

    @Column(name = "trang_thai", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
