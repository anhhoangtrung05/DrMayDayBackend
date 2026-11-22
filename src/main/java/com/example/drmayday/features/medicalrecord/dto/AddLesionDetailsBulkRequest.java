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

    @NotBlank(message = "Thiếu tham số bắt buộc: exam_id")
    @JsonProperty("exam_id")
    private String examId;

    @NotEmpty(message = "Trường items không được để trống")
    @Valid
    @JsonProperty("items")
    private List<LesionDetailItemRequest> items;
}
