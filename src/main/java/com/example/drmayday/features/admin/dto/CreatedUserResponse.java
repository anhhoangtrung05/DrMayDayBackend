package com.example.drmayday.features.admin.dto;

import com.example.drmayday.entity.UserAccount;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record CreatedUserResponse(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("person_id") Long personId,
        @JsonProperty("role_id") Long roleId,
        @JsonProperty("username") String username,
        @JsonProperty("is_active") Boolean isActive,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt) {

    public static CreatedUserResponse from(UserAccount account) {
        return new CreatedUserResponse(
                account.getAccountId(),
                account.getPerson().getPersonId(),
                account.getRole().getRoleId(),
                account.getUsername(),
                account.getActive(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
