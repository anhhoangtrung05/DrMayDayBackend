package com.example.drmayday.features.authentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "2001")
        @JsonProperty("current_password")
        String currentPassword,

        @NotBlank(message = "2001")
        @JsonProperty("new_password")
        String newPassword,

        @NotBlank(message = "2001")
        @JsonProperty("confirm_password")
        String confirmPassword
) {
}
