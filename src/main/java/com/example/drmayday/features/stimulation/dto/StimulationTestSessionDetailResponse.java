package com.example.drmayday.features.stimulation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.ALWAYS)
public class StimulationTestSessionDetailResponse {

    @JsonProperty("ma_phien_nghiem_phap")
    private Long maPhienNghiemPhap;

    @JsonProperty("ma_phien_kham")
    private Long maPhienKham;

    @JsonProperty("ngung_khang_histamine")
    private Boolean ngungKhangHistamine;

    @JsonProperty("thoi_gian_ngung_histamine")
    private Integer thoiGianNgungHistamine;

    @JsonProperty("ngung_corticoid")
    private Boolean ngungCorticoid;

    @JsonProperty("thoi_gian_ngung_corticoid")
    private Integer thoiGianNgungCorticoid;

    @JsonProperty("results")
    private List<StimulationTestResultResponse> results;
}
