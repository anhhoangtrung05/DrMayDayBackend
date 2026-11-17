package com.example.drmayday.features.profile.controller;

import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.UpdateProfileRequest;
import com.example.drmayday.features.profile.service.ProfileService;
import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for the authenticated patient's profile.
 * Handles PROFILE_GET and PROFILE_PUT.
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * PROFILE_GET
     * Retrieves the detailed profile of the authenticated patient.
     *
     * @param token Bearer token for the authenticated user
     * @return The patient's profile
     */
    @GetMapping
    public ResponseEntity<BaseResponse<PatientProfileResponse>> getProfile(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PatientProfileResponse response = profileService.getProfile(currentUserId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * PROFILE_PUT
     * Updates the patient's demographic, contact, and health insurance information.
     *
     * @param token Bearer token for the authenticated user
     * @param request Profile fields to update
     * @return The updated patient profile
     */
    @PutMapping
    public ResponseEntity<BaseResponse<PatientProfileResponse>> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PatientProfileResponse response = profileService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật thông tin thành công", response));
    }
}
