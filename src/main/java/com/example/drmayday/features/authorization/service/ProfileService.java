package com.example.drmayday.features.authorization.service;

import com.example.drmayday.features.authorization.dto.PatientProfileResponse;
import com.example.drmayday.features.authorization.dto.UpdateProfileRequest;

public interface ProfileService {

    /**
     * API 7: PROFILE_GET - Lấy thông tin hồ sơ chi tiết của bệnh nhân đang đăng nhập.
     *
     * @param userId ID tài khoản bệnh nhân
     * @return Hồ sơ chi tiết bệnh nhân
     */
    PatientProfileResponse getProfile(Long userId);

    default PatientProfileResponse getProfile(Integer userId) {
        return getProfile(userId != null ? userId.longValue() : null);
    }

    /**
     * API 8: PROFILE_PUT - Bệnh nhân cập nhật thông tin hồ sơ cá nhân.
     *
     * @param userId ID tài khoản bệnh nhân
     * @param request Dữ liệu cập nhật
     * @return Hồ sơ chi tiết sau khi cập nhật
     */
    PatientProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    default PatientProfileResponse updateProfile(Integer userId, UpdateProfileRequest request) {
        return updateProfile(userId != null ? userId.longValue() : null, request);
    }
}
