package com.example.drmayday.features.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authentication")
public record AuthenticationProperties(long passwordResetTokenExpirationMinutes) {
}
