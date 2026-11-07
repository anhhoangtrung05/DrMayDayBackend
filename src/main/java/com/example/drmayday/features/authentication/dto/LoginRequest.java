package com.example.drmayday.features.authentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "2001")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "2004")
        @JsonProperty("phone")
        String phone,

        @NotBlank(message = "2001")
        @JsonProperty("password")
        String password
) {
}
