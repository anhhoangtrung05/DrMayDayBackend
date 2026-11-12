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

/**
 * Controller for authentication and password management.
 * Handles AUTH_LOGIN, AUTH_LOGOUT, AUTH_FORGOT_PASSWORD, AUTH_RESET_PASSWORD, and AUTH_CHANGE_PASSWORD.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * AUTH_LOGIN
     * Authenticates a user and returns a JWT with basic user information.
     *
     * @param request User phone number and password
     * @return The authentication token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(BaseResponse.ok(authService.login(request)));
    }

    /**
     * AUTH_LOGOUT
     * Ends the current session using the supplied bearer token.
     *
     * @param authorizationHeader Current bearer token
     * @return A success response
     */
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok(BaseResponse.success("Đăng xuất thành công", null));
    }

    /**
     * AUTH_FORGOT_PASSWORD
     * Starts password recovery for the account associated with the supplied phone number.
     *
     * @param request Phone number to receive password recovery instructions
     * @return A response confirming that recovery instructions were sent
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(BaseResponse.success("Đã gửi hướng dẫn khôi phục mật khẩu", null));
    }

    /**
     * AUTH_RESET_PASSWORD
     * Validates the recovery code and sets a new password.
     *
     * @param request Phone number, recovery code, new password, and confirmation
     * @return A response confirming that the password was reset
     */
    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<Map<String, Object>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(BaseResponse.success("Đặt lại mật khẩu thành công", Map.of()));
    }

    /**
     * AUTH_CHANGE_PASSWORD
     * Changes the password for the authenticated user.
     *
     * @param authorizationHeader Current bearer token
     * @param request Current password, new password, and confirmation
     * @return A response confirming that the password was changed
     */
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> changePassword(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authorizationHeader, request);
        return ResponseEntity.ok(BaseResponse.success("Đổi mật khẩu thành công", null));
    }
}
