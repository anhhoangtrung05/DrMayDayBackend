package com.example.drmayday.features.auth.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.auth.dto.ChangePasswordRequest;
import com.example.drmayday.features.auth.dto.ForgotPasswordRequest;
import com.example.drmayday.features.auth.dto.LoginRequest;
import com.example.drmayday.features.auth.dto.LoginResponse;
import com.example.drmayday.features.auth.dto.ResetPasswordRequest;
import com.example.drmayday.features.auth.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(BaseResponse.ok(authService.login(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok(BaseResponse.success("Logout successful", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(BaseResponse.success("Password recovery instructions sent", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<Map<String, Object>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(BaseResponse.success("Password reset successful", Map.of()));
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> changePassword(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authorizationHeader, request);
        return ResponseEntity.ok(BaseResponse.success("Password changed successfully", null));
    }
}
