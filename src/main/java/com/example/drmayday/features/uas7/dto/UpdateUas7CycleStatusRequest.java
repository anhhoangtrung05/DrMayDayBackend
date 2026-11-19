package com.example.drmayday.features.uas7.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUas7CycleStatusRequest {
    @NotBlank(message = "2001") @JsonProperty("trang_thai_chu_ky") private String cycleStatus;
}
