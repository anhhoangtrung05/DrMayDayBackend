package com.example.drmayday.features.uas7.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUas7CycleStatusRequest {

    @JsonProperty("trang_thai_chu_ky")
    @JsonAlias({"cycleStatus", "cycle_status"})
    @NotBlank(message = "2001")
    private String trangThaiChuKy;
}
