package com.example.drmayday.features.authentication.service;

import com.example.drmayday.features.authentication.dto.LoginRequest;
import com.example.drmayday.features.authentication.dto.LoginResponse;
import com.example.drmayday.features.authentication.dto.ForgotPasswordRequest;
import com.example.drmayday.features.authentication.dto.ChangePasswordRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    void logout(String authorizationHeader);

    void forgotPassword(ForgotPasswordRequest request);

    void changePassword(String authorizationHeader, ChangePasswordRequest request);
}
