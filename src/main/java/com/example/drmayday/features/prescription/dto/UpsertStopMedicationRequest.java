package com.example.drmayday.features.prescription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** instructions replaces the whole existing list; an empty array clears it. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertStopMedicationRequest {

    @JsonProperty("don_thuoc_id")
    private Long donThuocId;

    @JsonProperty("instructions")
    private List<StopInstructionRequest> instructions;
}
