package com.example.drmayday.features.prescription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One medication line. Exactly one of ma_thuoc / ten_thuoc_bo_sung must be set. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemRequest {

    @JsonProperty("ma_thuoc")
    private String maThuoc;

    @JsonProperty("ten_thuoc_bo_sung")
    private String tenThuocBoSung;

    @JsonProperty("lieu_dung")
    private String lieuDung;

    @JsonProperty("thoi_gian_dung")
    private String thoiGianDung;

    @JsonProperty("ma_ke_hoach_dieu_tri")
    private String maKeHoachDieuTri;
}
