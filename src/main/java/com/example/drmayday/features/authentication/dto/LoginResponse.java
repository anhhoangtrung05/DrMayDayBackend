package com.example.drmayday.features.authentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
        String token,
        @JsonProperty("user_info") UserInfo userInfo
) {
    public record UserInfo(
            @JsonProperty("user_id") Long userId,
            @JsonProperty("full_name") String fullName,
            String phone,
            @JsonProperty("role_id") Long roleId
    ) {
    }
}
