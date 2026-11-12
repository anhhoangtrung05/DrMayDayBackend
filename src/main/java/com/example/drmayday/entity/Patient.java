package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "benhnhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {
    @Id
    @Column(name = "ma_benh_nhan")
    private Long patientId;

    @Column(name = "ma_his", length = 50)
    private String hisCode;

    @Column(name = "nghe_nghiep", length = 40)
    private String occupation;

    @Column(name = "dan_toc", length = 50)
    private String ethnicity;

    @Column(name = "bhyt_so_the", length = 30)
    private String healthInsuranceNumber;

    @Column(name = "bhyt_gia_tri_den")
    private LocalDate healthInsuranceValidUntil;

    @Column(name = "nguoi_nha_ho_ten_dia_chi")
    private String emergencyContactNameAndAddress;

    @Column(name = "nguoi_nha_so_dien_thoai", length = 10)
    private String emergencyContactPhone;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "ma_benh_nhan", referencedColumnName = "ma_nguoi")
    private Person person;
}
