package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalVisit;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bác sĩ của phiên khám (API 47, 48). Cần employee.person đã được nạp.
 */
public record ExamDoctorDto(
        @JsonProperty("ma_bac_si") String physicianId,
        @JsonProperty("ho_ten_bac_si") String fullName) {

    public static ExamDoctorDto from(MedicalVisit v) {
        return new ExamDoctorDto(JsonValues.id(v.getPhysicianId()), v.getEmployee().getPerson().getFullName());
    }
}
