package com.example.drmayday.features.profile.service;

import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.UpdateProfileRequest;

public interface ProfileService {
    PatientProfileResponse getProfile(Long userId);

    default PatientProfileResponse getProfile(Integer userId) {
        return getProfile(userId != null ? userId.longValue() : null);
    }

    PatientProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    default PatientProfileResponse updateProfile(Integer userId, UpdateProfileRequest request) {
        return updateProfile(userId != null ? userId.longValue() : null, request);
    }
}
