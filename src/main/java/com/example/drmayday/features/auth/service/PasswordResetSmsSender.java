package com.example.drmayday.features.auth.service;

public interface PasswordResetSmsSender {
    void send(String phone, String code, long expirationMinutes);
}
