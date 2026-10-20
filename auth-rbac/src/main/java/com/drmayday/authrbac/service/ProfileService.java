package com.drmayday.authrbac.service;

import com.drmayday.authrbac.dto.PatientProfileResponse;
import com.drmayday.authrbac.dto.UpdateProfileRequest;

public interface ProfileService {

    /**
     * API 7: PROFILE_GET - Lấy thông tin hồ sơ chi tiết của bệnh nhân đang đăng nhập.
     *
     * @param userId ID tài khoản bệnh nhân
     * @return Hồ sơ chi tiết bệnh nhân
     */
    PatientProfileResponse getProfile(Integer userId);

    /**
     * API 8: PROFILE_PUT - Bệnh nhân cập nhật thông tin hồ sơ cá nhân.
     *
     * @param userId ID tài khoản bệnh nhân
     * @param request Dữ liệu cập nhật
     * @return Hồ sơ chi tiết sau khi cập nhật
     */
    PatientProfileResponse updateProfile(Integer userId, UpdateProfileRequest request);
}
