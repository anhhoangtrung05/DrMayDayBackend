package com.example.drmayday.features.authentication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "speedsms")
public record SpeedSmsProperties(
        String apiUrl,
        String apiToken,
        int smsType,
        String sender,
        String passwordResetMessage
) {
}
