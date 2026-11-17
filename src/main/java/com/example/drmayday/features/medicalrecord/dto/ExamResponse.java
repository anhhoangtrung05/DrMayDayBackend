package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalVisit;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Phiên khám vừa tạo hoặc vừa sửa (API 46, 49). Id là chuỗi, nhiệt độ luôn một chữ số thập phân.
 */
public record ExamResponse(
        @JsonProperty("exam_id") String examId,
        @JsonProperty("record_id") String recordId,
        @JsonProperty("ma_bac_si") String physicianId,
        @JsonProperty("ma_phong_kham") String clinicRoomId,
        @JsonProperty("loai_benh_an") String medicalRecordType,
        @JsonProperty("ngay_kham") String visitDate,
        @JsonProperty("lan_kham_thu") Integer visitNumber,
        @JsonProperty("loai_phien_kham") String visitType,
        @JsonProperty("luu_huyet_thanh") Boolean serumSampleStored,
        @JsonProperty("sot") Boolean fever,
        @JsonProperty("sot_nhiet_do_c") BigDecimal feverTemperatureCelsius,
        @JsonProperty("ngay_hen_tai_kham") String followUpDate,
        @JsonProperty("tom_tat_buoi_kham") String visitSummary,
        @JsonProperty("huong_xu_tri_lan_sau") String nextVisitManagement,
        @JsonProperty("dan_do") String instructions) {

    public static ExamResponse from(MedicalVisit v) {
        return new ExamResponse(
                JsonValues.id(v.getMedicalVisitId()),
                JsonValues.id(v.getMedicalRecordId()),
                JsonValues.id(v.getPhysicianId()),
                JsonValues.id(v.getClinicRoomId()),
                v.getMedicalRecordType(),
                JsonValues.date(v.getVisitDate()),
                v.getVisitNumber(),
                v.getVisitType(),
                v.getSerumSampleStored(),
                v.getFever(),
                JsonValues.oneDecimal(v.getFeverTemperatureCelsius()),
                JsonValues.date(v.getFollowUpDate()),
                v.getVisitSummary(),
                v.getNextVisitManagement(),
                v.getInstructions());
    }
}
