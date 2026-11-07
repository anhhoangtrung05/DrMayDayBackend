package com.example.drmayday.features.authentication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authentication")
public record AuthenticationProperties(long passwordResetTokenExpirationMinutes) {
}
