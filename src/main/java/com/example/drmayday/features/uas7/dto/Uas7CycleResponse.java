package com.example.drmayday.features.uas7.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data @Builder
public class Uas7CycleResponse {
    @JsonProperty("cycle_id") private String cycleId;
    @JsonProperty("patient_id") private String patientId;
    @JsonProperty("ngay_bat_dau") private LocalDate startDate;
    @JsonProperty("ngay_ket_thuc") private LocalDate endDate;
    @JsonProperty("trang_thai_chu_ky") private String cycleStatus;
    @JsonProperty("so_ngay_hop_le") private int validDays;
    @JsonProperty("thoi_diem_tao") private OffsetDateTime createdAt;
    @JsonProperty("thoi_diem_cap_nhat") private OffsetDateTime updatedAt;
}
