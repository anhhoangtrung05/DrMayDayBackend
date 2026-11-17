package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalVisit;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Một phiên trong danh sách phiên khám của hồ sơ (API 47). Cần employee.person và clinicRoom đã được nạp.
 */
@Getter
public class ExamSummaryResponse {

    @JsonProperty("exam_id")
    private final String examId;

    @JsonProperty("record_id")
    private final String recordId;

    @JsonProperty("lan_kham_thu")
    private final Integer visitNumber;

    @JsonProperty("ngay_kham")
    private final String visitDate;

    @JsonProperty("loai_phien_kham")
    private final String visitType;

    @JsonProperty("bac_si")
    private final ExamDoctorDto doctor;

    @JsonProperty("phong_kham")
    private final ExamRoomDto room;

    @JsonProperty("luu_huyet_thanh")
    private final Boolean serumSampleStored;

    @JsonProperty("sot")
    private final Boolean fever;

    @JsonProperty("sot_nhiet_do_c")
    private final BigDecimal feverTemperatureCelsius;

    @JsonProperty("ngay_hen_tai_kham")
    private final String followUpDate;

    @JsonProperty("tom_tat_buoi_kham")
    private final String visitSummary;

    public ExamSummaryResponse(MedicalVisit v) {
        this.examId = JsonValues.id(v.getMedicalVisitId());
        this.recordId = JsonValues.id(v.getMedicalRecordId());
        this.visitNumber = v.getVisitNumber();
        this.visitDate = JsonValues.date(v.getVisitDate());
        this.visitType = v.getVisitType();
        this.doctor = ExamDoctorDto.from(v);
        this.room = ExamRoomDto.from(v.getClinicRoom());
        this.serumSampleStored = v.getSerumSampleStored();
        this.fever = v.getFever();
        this.feverTemperatureCelsius = JsonValues.oneDecimal(v.getFeverTemperatureCelsius());
        this.followUpDate = JsonValues.date(v.getFollowUpDate());
        this.visitSummary = v.getVisitSummary();
    }
}
