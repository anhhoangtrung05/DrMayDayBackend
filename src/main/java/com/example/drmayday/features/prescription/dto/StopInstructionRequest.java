package com.example.drmayday.features.prescription.dto;

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
public class StopInstructionRequest {

    @JsonProperty("thuoc_can_ngung")
    private String thuocCanNgung;

    @JsonProperty("ngung_tu_ngay")
    private LocalDate ngungTuNgay;

    @JsonProperty("thoi_gian_ngung_ngay")
    private Integer thoiGianNgungNgay;

    /** One of: Test huyet thanh tu than | Test kich thich | Khac. */
    @JsonProperty("muc_dich")
    private String mucDich;

    /** Required when muc_dich = Khac, must be absent otherwise. */
    @JsonProperty("muc_dich_khac_ghi_ro")
    private String mucDichKhacGhiRo;
}
