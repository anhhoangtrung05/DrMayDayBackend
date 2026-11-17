package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalVisit;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Một phiên khám trong lịch sử khám của bệnh nhân (API 39).
 */
public record ExamHistoryResponse(
        @JsonProperty("record_id") String recordId,
        @JsonProperty("loai_benh_an") String medicalRecordType,
        @JsonProperty("exam_id") String examId,
        @JsonProperty("lan_kham_thu") Integer visitNumber,
        @JsonProperty("ngay_kham") String visitDate,
        @JsonProperty("ma_bac_si") String physicianId,
        @JsonProperty("loai_phien_kham") String visitType,
        @JsonProperty("ngay_hen_tai_kham") String followUpDate) {

    public static ExamHistoryResponse from(MedicalVisit visit) {
        return new ExamHistoryResponse(
                JsonValues.id(visit.getMedicalRecordId()),
                visit.getMedicalRecordType(),
                JsonValues.id(visit.getMedicalVisitId()),
                visit.getVisitNumber(),
                JsonValues.date(visit.getVisitDate()),
                JsonValues.id(visit.getPhysicianId()),
                visit.getVisitType(),
                JsonValues.date(visit.getFollowUpDate()));
    }
}
