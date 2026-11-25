package com.example.drmayday.features.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @JsonProperty("person_id")
        @NotNull(message = "2001")
        Long personId,

        @JsonProperty("role_id")
        @NotNull(message = "2001")
        Long roleId,

        @JsonProperty("username")
        @NotBlank(message = "2001")
        @Size(max = 100, message = "2003")
        String username,

        @JsonProperty("password")
        @NotBlank(message = "2001")
        String password,

        @JsonProperty("is_active")
        Boolean isActive) {
}
