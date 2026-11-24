package com.example.drmayday.features.profile.controller;

import com.example.drmayday.features.profile.dto.EmployeeProfileResponse;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.PersonProfileResponse;
import com.example.drmayday.features.profile.dto.UpdateEmployeeProfileRequest;
import com.example.drmayday.features.profile.dto.UpdatePatientProfileRequest;
import com.example.drmayday.features.profile.dto.UpdatePersonProfileRequest;
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

    @GetMapping("/person")
    public ResponseEntity<BaseResponse<PersonProfileResponse>> getPersonProfile() {
        PersonProfileResponse response = profileService.getPersonProfile(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/person")
    public ResponseEntity<BaseResponse<PersonProfileResponse>> updatePersonProfile(
            @Valid @RequestBody UpdatePersonProfileRequest request) {
        PersonProfileResponse response = profileService.updatePersonProfile(
                SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật thông tin thành công", response));
    }

    @GetMapping("/patient")
    public ResponseEntity<BaseResponse<PatientProfileResponse>> getPatientProfile() {
        PatientProfileResponse response = profileService.getPatientProfile(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/patient")
    public ResponseEntity<BaseResponse<PatientProfileResponse>> updatePatientProfile(
            @Valid @RequestBody UpdatePatientProfileRequest request) {
        PatientProfileResponse response = profileService.updatePatientProfile(
                SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật thông tin Bệnh nhân thành công", response));
    }

    @GetMapping("/employee")
    public ResponseEntity<BaseResponse<EmployeeProfileResponse>> getEmployeeProfile() {
        EmployeeProfileResponse response = profileService.getEmployeeProfile(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/employee")
    public ResponseEntity<BaseResponse<EmployeeProfileResponse>> updateEmployeeProfile(
            @Valid @RequestBody UpdateEmployeeProfileRequest request) {
        EmployeeProfileResponse response = profileService.updateEmployeeProfile(
                SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật thông tin Nhân viên thành công", response));
    }
}
