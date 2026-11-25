package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddVitalSignsRequest {

    @NotBlank(message = "Thiếu tham số bắt buộc: exam_id")
    @JsonProperty("exam_id")
    private String examId;

    @JsonProperty("thoi_diem_do")
    private OffsetDateTime thoiDiemDo;

    @JsonProperty("mach_lan_phut")
    private Integer machLanPhut;

    @JsonProperty("huyet_ap_tam_thu")
    private Integer huyetApTamThu;

    @JsonProperty("huyet_ap_tam_truong")
    private Integer huyetApTamTruong;

    @JsonProperty("nhip_tho_lan_phut")
    private Integer nhipThoLanPhut;

    @JsonProperty("spo2_phan_tram")
    private BigDecimal spo2PhanTram;

    @JsonProperty("co_bat_thuong_co_quan")
    private Boolean coBatThuongCoQuan;

    @JsonProperty("bat_thuong_co_quan_ghi_ro")
    private String batThuongCoQuanGhiRo;
}
