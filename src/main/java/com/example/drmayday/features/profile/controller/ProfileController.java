package com.example.drmayday.features.profile.controller;

import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.UpdateProfileRequest;
import com.example.drmayday.features.profile.service.ProfileService;
import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<BaseResponse<PatientProfileResponse>> getProfile(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PatientProfileResponse response = profileService.getProfile(currentUserId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping
    public ResponseEntity<BaseResponse<PatientProfileResponse>> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PatientProfileResponse response = profileService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật thông tin thành công", response));
    }
}
