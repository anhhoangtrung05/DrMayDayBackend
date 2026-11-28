package com.example.drmayday.features.uas7.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Uas7CycleResponse {

    @JsonProperty("cycle_id")
    private String cycleId;

    @JsonProperty("patient_id")
    private String patientId;

    @JsonProperty("ngay_bat_dau")
    private LocalDate ngayBatDau;

    @JsonProperty("ngay_ket_thuc")
    private LocalDate ngayKetThuc;

    @JsonProperty("trang_thai_chu_ky")
    private String trangThaiChuKy;

    @JsonProperty("so_ngay_hop_le")
    private Integer soNgayHopLe;

    @JsonProperty("thoi_diem_tao")
    private OffsetDateTime thoiDiemTao;

    @JsonProperty("thoi_diem_cap_nhat")
    private OffsetDateTime thoiDiemCapNhat;
}
