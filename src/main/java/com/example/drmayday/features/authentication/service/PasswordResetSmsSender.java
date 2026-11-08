package com.example.drmayday.features.authentication.service;

public interface PasswordResetSmsSender {
    void send(String phone, String code, long expirationMinutes);
}
