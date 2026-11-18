package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddLesionDetailsBulkRequest {

    @NotBlank(message = "exam_id is required")
    @JsonProperty("exam_id")
    private String examId;

    @NotEmpty(message = "items cannot be empty")
    @Valid
    @JsonProperty("items")
    private List<LesionDetailItemRequest> items;
}
