package com.example.drmayday.features.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePatientRequest(
        @JsonProperty("person_id")
        @NotNull(message = "2001")
        Long personId,

        @JsonProperty("his_code")
        @Pattern(regexp = ".*\\S.*", message = "2003")
        @Size(max = 50, message = "2003")
        String hisCode,

        @JsonProperty("occupation")
        @Pattern(regexp = "^(Cong nhan|Nong dan|Hoc sinh - Sinh vien|Can bo cong chuc|Khac)$", message = "2003")
        String occupation,

        @JsonProperty("ethnicity")
        @Size(max = 50, message = "2003")
        String ethnicity,

        @JsonProperty("health_insurance_number")
        @Pattern(regexp = ".*\\S.*", message = "2003")
        @Size(max = 30, message = "2003")
        String healthInsuranceNumber,

        @JsonProperty("health_insurance_valid_until")
        LocalDate healthInsuranceValidUntil,

        @JsonProperty("emergency_contact_name_address")
        String emergencyContactNameAddress,

        @JsonProperty("emergency_contact_phone")
        @Pattern(regexp = "^[0-9]{10}$", message = "2004")
        String emergencyContactPhone) {
}
