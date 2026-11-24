package com.example.drmayday.features.profile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdatePersonProfileRequest {

    @JsonProperty("full_name")
    @Pattern(regexp = ".*\\S.*", message = "2003")
    @Size(max = 150, message = "2003")
    private String fullName;

    @JsonProperty("date_of_birth")
    @PastOrPresent(message = "2003")
    private LocalDate dateOfBirth;

    @JsonProperty("gender")
    @Pattern(regexp = "^(Nam|Nu)$", message = "2003")
    private String gender;

    @JsonProperty("phone_number")
    @Pattern(regexp = "^[0-9]{10}$", message = "2004")
    private String phoneNumber;

    @JsonProperty("province_city")
    @Size(max = 100, message = "2003")
    private String provinceCity;

    @JsonProperty("ward_commune")
    @Size(max = 100, message = "2003")
    private String wardCommune;

    @JsonProperty("address_line")
    @Size(max = 255, message = "2003")
    private String addressLine;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProvinceCity() {
        return provinceCity;
    }

    public void setProvinceCity(String provinceCity) {
        this.provinceCity = provinceCity;
    }

    public String getWardCommune() {
        return wardCommune;
    }

    public void setWardCommune(String wardCommune) {
        this.wardCommune = wardCommune;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public boolean hasUpdates() {
        return fullName != null || dateOfBirth != null || gender != null || phoneNumber != null
                || provinceCity != null || wardCommune != null || addressLine != null;
    }
}
