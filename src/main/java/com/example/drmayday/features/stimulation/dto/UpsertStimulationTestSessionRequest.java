package com.example.drmayday.features.stimulation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertStimulationTestSessionRequest {

    @JsonProperty("ngung_khang_histamine")
    @JsonAlias({"ngungKhangHistamine", "stoppedAntihistamines"})
    private Boolean ngungKhangHistamine;

    @JsonProperty("thoi_gian_ngung_histamine")
    @JsonAlias({"thoiGianNgungHistamine", "histamineDiscontinuationDays"})
    private Integer thoiGianNgungHistamine;

    @JsonProperty("ngung_corticoid")
    @JsonAlias({"ngungCorticoid", "stoppedCorticosteroids"})
    private Boolean ngungCorticoid;

    @JsonProperty("thoi_gian_ngung_corticoid")
    @JsonAlias({"thoiGianNgungCorticoid", "corticosteroidDiscontinuationDays"})
    private Integer thoiGianNgungCorticoid;
}
