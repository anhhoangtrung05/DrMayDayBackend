package com.example.drmayday.features.notification.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetDevTokenRequest {

    @JsonProperty("fcm_token")
    @JsonAlias({"fcmToken"})
    private String fcmToken;

    @JsonProperty("device_type")
    @JsonAlias({"deviceType"})
    private String deviceType;
}
