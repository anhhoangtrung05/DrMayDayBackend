package com.example.drmayday.features.prescription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Only the fields that are sent are updated; items, when sent, replace every existing item. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePrescriptionRequest {

    @JsonProperty("trang_thai_don")
    private String trangThaiDon;

    @JsonProperty("ghi_chu_dieu_tri")
    private String ghiChuDieuTri;

    @JsonProperty("items")
    private List<PrescriptionItemRequest> items;
}
