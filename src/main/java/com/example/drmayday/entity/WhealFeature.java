package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dacdiemsanphu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhealFeature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dac_diem_san_phu", nullable = false) private Long whealFeatureId;
    @Column(name = "ten_dac_diem", nullable = false, length = 150) private String name;
    @Column(name = "mo_ta") private String description;
}
