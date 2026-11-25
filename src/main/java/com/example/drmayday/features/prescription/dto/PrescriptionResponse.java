package com.example.drmayday.features.prescription.dto;

import com.example.drmayday.entity.Prescription;
import com.example.drmayday.entity.PrescriptionItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponse {

    @JsonProperty("prescription_id")
    private String prescriptionId;

    @JsonProperty("record_id")
    private String recordId;

    @JsonProperty("phien_kham_id")
    private String phienKhamId;

    @JsonProperty("ngay_ke_don")
    private LocalDate ngayKeDon;

    @JsonProperty("trang_thai_don")
    private String trangThaiDon;

    @JsonProperty("ghi_chu_dieu_tri")
    private String ghiChuDieuTri;

    @JsonProperty("ngung_thuoc_lan_sau")
    private Boolean ngungThuocLanSau;

    @JsonProperty("items")
    private List<PrescriptionItemResponse> items;

    public static PrescriptionResponse from(Prescription prescription, List<PrescriptionItem> items) {
        return PrescriptionResponse.builder()
                .prescriptionId(String.valueOf(prescription.getPrescriptionId()))
                .recordId(String.valueOf(prescription.getMedicalRecordId()))
                .phienKhamId(String.valueOf(prescription.getMedicalVisitId()))
                .ngayKeDon(prescription.getPrescriptionDate())
                .trangThaiDon(prescription.getPrescriptionStatus())
                .ghiChuDieuTri(prescription.getTreatmentNotes())
                .ngungThuocLanSau(Boolean.TRUE.equals(prescription.getStopMedicationAtNextVisit()))
                .items(items.stream().map(PrescriptionItemResponse::from).toList())
                .build();
    }
}
