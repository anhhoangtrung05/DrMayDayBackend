package com.example.drmayday.features.profile.dto;

import com.example.drmayday.entity.Patient;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record PatientProfileResponse(
        @JsonProperty("person_id") Long personId,
        @JsonProperty("his_code") String hisCode,
        @JsonProperty("occupation") String occupation,
        @JsonProperty("ethnicity") String ethnicity,
        @JsonProperty("health_insurance_number") String healthInsuranceNumber,
        @JsonProperty("health_insurance_valid_until") LocalDate healthInsuranceValidUntil,
        @JsonProperty("emergency_contact_name_address") String emergencyContactNameAddress,
        @JsonProperty("emergency_contact_phone") String emergencyContactPhone) {

    public static PatientProfileResponse from(Patient patient) {
        return new PatientProfileResponse(
                patient.getPatientId(),
                patient.getHisCode(),
                patient.getOccupation(),
                patient.getEthnicity(),
                patient.getHealthInsuranceNumber(),
                patient.getHealthInsuranceValidUntil(),
                patient.getEmergencyContactNameAndAddress(),
                patient.getEmergencyContactPhone());
    }
}
