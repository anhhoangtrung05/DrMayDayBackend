package com.example.drmayday.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "2001")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "2004")
        @JsonProperty("phone")
        String phone,

        @NotBlank(message = "2001")
        @JsonProperty("reset_token")
        String resetToken,

        @NotBlank(message = "2001")
        @JsonProperty("new_password")
        String newPassword,

        @NotBlank(message = "2001")
        @JsonProperty("confirm_password")
        String confirmPassword
) {
}
