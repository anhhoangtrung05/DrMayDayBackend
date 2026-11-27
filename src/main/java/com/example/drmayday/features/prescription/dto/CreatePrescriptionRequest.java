package com.example.drmayday.features.prescription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Required fields are checked in the service so each error carries a specific message. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrescriptionRequest {

    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("phien_kham_id")
    private Long phienKhamId;

    /** Defaults to today when omitted. */
    @JsonProperty("ngay_ke_don")
    private LocalDate ngayKeDon;

    /** Defaults to DANG_DIEU_TRI when omitted. */
    @JsonProperty("trang_thai_don")
    private String trangThaiDon;

    @JsonProperty("ghi_chu_dieu_tri")
    private String ghiChuDieuTri;

    @JsonProperty("items")
    private List<PrescriptionItemRequest> items;
}
