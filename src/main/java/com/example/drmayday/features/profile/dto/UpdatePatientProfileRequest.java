package com.example.drmayday.features.profile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdatePatientProfileRequest {

    @JsonProperty("person_id")
    @NotNull(message = "2001")
    private Long personId;

    @JsonProperty("his_code")
    @Size(max = 50, message = "2003")
    private String hisCode;

    @JsonProperty("occupation")
    @Pattern(regexp = "^(Cong nhan|Nong dan|Hoc sinh - Sinh vien|Can bo cong chuc|Khac)$", message = "2003")
    private String occupation;

    @JsonProperty("ethnicity")
    @Size(max = 50, message = "2003")
    private String ethnicity;

    @JsonProperty("health_insurance_number")
    @Size(max = 30, message = "2003")
    private String healthInsuranceNumber;

    @JsonProperty("health_insurance_valid_until")
    private LocalDate healthInsuranceValidUntil;

    @JsonProperty("emergency_contact_name_address")
    private String emergencyContactNameAddress;

    @JsonProperty("emergency_contact_phone")
    @Pattern(regexp = "^[0-9]{10}$", message = "2004")
    private String emergencyContactPhone;

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public String getHisCode() {
        return hisCode;
    }

    public void setHisCode(String hisCode) {
        this.hisCode = hisCode;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getEthnicity() {
        return ethnicity;
    }

    public void setEthnicity(String ethnicity) {
        this.ethnicity = ethnicity;
    }

    public String getHealthInsuranceNumber() {
        return healthInsuranceNumber;
    }

    public void setHealthInsuranceNumber(String healthInsuranceNumber) {
        this.healthInsuranceNumber = healthInsuranceNumber;
    }

    public LocalDate getHealthInsuranceValidUntil() {
        return healthInsuranceValidUntil;
    }

    public void setHealthInsuranceValidUntil(LocalDate healthInsuranceValidUntil) {
        this.healthInsuranceValidUntil = healthInsuranceValidUntil;
    }

    public String getEmergencyContactNameAddress() {
        return emergencyContactNameAddress;
    }

    public void setEmergencyContactNameAddress(String emergencyContactNameAddress) {
        this.emergencyContactNameAddress = emergencyContactNameAddress;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public boolean hasUpdates() {
        return hisCode != null || occupation != null || ethnicity != null || healthInsuranceNumber != null
                || healthInsuranceValidUntil != null || emergencyContactNameAddress != null
                || emergencyContactPhone != null;
    }
}
