package com.example.drmayday.features.uas7.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateUas7CycleRequest {
    @NotNull(message = "2001") @JsonProperty("ngay_bat_dau") private LocalDate startDate;
}
