package com.example.drmayday.features.uas7.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUas7CycleRequest {

    @JsonProperty("ngay_bat_dau")
    @JsonAlias({"startDate", "start_date"})
    @NotNull(message = "2001")
    private LocalDate ngayBatDau;
}
