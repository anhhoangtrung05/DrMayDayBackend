package com.example.drmayday.features.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record CreateStaffRequest(
        @JsonProperty("person_id")
        @NotNull(message = "2001")
        Long personId,

        @JsonProperty("branch_id")
        @NotNull(message = "2001")
        Long branchId) {
}
