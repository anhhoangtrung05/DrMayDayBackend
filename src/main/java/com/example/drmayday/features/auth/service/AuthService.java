package com.example.drmayday.features.auth.service;

import com.example.drmayday.features.auth.dto.ChangePasswordRequest;
import com.example.drmayday.features.auth.dto.ForgotPasswordRequest;
import com.example.drmayday.features.auth.dto.LoginRequest;
import com.example.drmayday.features.auth.dto.LoginResponse;
import com.example.drmayday.features.auth.dto.ResetPasswordRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    void logout(String authorizationHeader);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(String authorizationHeader, ChangePasswordRequest request);
}
