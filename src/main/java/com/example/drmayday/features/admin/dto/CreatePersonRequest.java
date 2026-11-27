package com.example.drmayday.features.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePersonRequest(
        @JsonProperty("full_name")
        @NotBlank(message = "2001")
        @Size(max = 150, message = "2003")
        String fullName,

        @JsonProperty("phone_number")
        @NotBlank(message = "2001")
        @Pattern(regexp = "^(?:[0-9]{10})?$", message = "2004")
        String phoneNumber,

        @JsonProperty("date_of_birth")
        @PastOrPresent(message = "2003")
        LocalDate dateOfBirth,

        @JsonProperty("gender")
        @Pattern(regexp = "^(Nam|Nu)$", message = "2003")
        String gender,

        @JsonProperty("province_city")
        @Size(max = 100, message = "2003")
        String provinceCity,

        @JsonProperty("ward_commune")
        @Size(max = 100, message = "2003")
        String wardCommune,

        @JsonProperty("address_line")
        @Size(max = 255, message = "2003")
        String addressLine) {
}
