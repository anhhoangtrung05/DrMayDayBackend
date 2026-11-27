package com.example.drmayday.features.prescription.dto;

import com.example.drmayday.entity.MedicationDiscontinuation;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopInstructionResponse {

    @JsonProperty("chi_dan_id")
    private String chiDanId;

    @JsonProperty("thuoc_can_ngung")
    private String thuocCanNgung;

    @JsonProperty("ngung_tu_ngay")
    private LocalDate ngungTuNgay;

    @JsonProperty("thoi_gian_ngung_ngay")
    private Integer thoiGianNgungNgay;

    @JsonProperty("muc_dich")
    private String mucDich;

    @JsonProperty("muc_dich_khac_ghi_ro")
    private String mucDichKhacGhiRo;

    public static StopInstructionResponse from(MedicationDiscontinuation row) {
        return StopInstructionResponse.builder()
                .chiDanId(row.getDiscontinuationId() == null ? null : String.valueOf(row.getDiscontinuationId()))
                .thuocCanNgung(row.getMedicationToStop())
                .ngungTuNgay(row.getStopDate())
                .thoiGianNgungNgay(row.getDiscontinuationDays())
                .mucDich(row.getPurpose())
                .mucDichKhacGhiRo(row.getOtherPurposeDescription())
                .build();
    }
}
