package com.example.drmayday.features.prescription.dto;

import com.example.drmayday.entity.MedicationDiscontinuation;
import com.example.drmayday.entity.Prescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopMedicationResponse {

    @JsonProperty("don_thuoc_id")
    private String donThuocId;

    @JsonProperty("ngung_thuoc_lan_sau")
    private Boolean ngungThuocLanSau;

    @JsonProperty("instructions")
    private List<StopInstructionResponse> instructions;

    public static StopMedicationResponse from(Prescription prescription, List<MedicationDiscontinuation> rows) {
        return StopMedicationResponse.builder()
                .donThuocId(String.valueOf(prescription.getPrescriptionId()))
                .ngungThuocLanSau(Boolean.TRUE.equals(prescription.getStopMedicationAtNextVisit()))
                .instructions(rows.stream().map(StopInstructionResponse::from).toList())
                .build();
    }
}
