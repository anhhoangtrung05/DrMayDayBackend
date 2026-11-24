package com.example.drmayday.features.prescription.dto;

import com.example.drmayday.entity.PrescriptionItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemResponse {

    @JsonProperty("chi_tiet_id")
    private String chiTietId;

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

    public static PrescriptionItemResponse from(PrescriptionItem item) {
        return PrescriptionItemResponse.builder()
                .chiTietId(item.getPrescriptionItemId() == null ? null : String.valueOf(item.getPrescriptionItemId()))
                .maThuoc(item.getMedicationId())
                .tenThuocBoSung(item.getAdditionalMedicationName())
                .lieuDung(item.getDosage())
                .thoiGianDung(item.getUsageDuration())
                .maKeHoachDieuTri(item.getTreatmentPlanId())
                .build();
    }
}
