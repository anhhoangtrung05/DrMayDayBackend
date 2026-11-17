package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO sửa phiên khám (API 49), kiểu gộp: trường vắng mặt hoặc null giữ nguyên giá trị hiện tại.
 * Không có record_id, lan_kham_thu, loai_benh_an, ma_bac_si: gửi lên bị bỏ qua vì Spring Boot bỏ qua trường lạ.
 * Không có annotation Bean Validation, mọi kiểm tra nằm trong service.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateExamRequest {

    // Chuỗi để service tự trả 2002 khi không phải số
    @JsonProperty("ma_phong_kham")
    private String clinicRoomId;

    @JsonProperty("ngay_kham")
    private LocalDate visitDate;

    @JsonProperty("loai_phien_kham")
    private String visitType;

    @JsonProperty("luu_huyet_thanh")
    private Boolean serumSampleStored;

    @JsonProperty("sot")
    private Boolean fever;

    @JsonProperty("sot_nhiet_do_c")
    private BigDecimal feverTemperatureCelsius;

    @JsonProperty("ngay_hen_tai_kham")
    private LocalDate followUpDate;

    @JsonProperty("tom_tat_buoi_kham")
    private String visitSummary;

    @JsonProperty("huong_xu_tri_lan_sau")
    private String nextVisitManagement;

    @JsonProperty("dan_do")
    private String instructions;
}
