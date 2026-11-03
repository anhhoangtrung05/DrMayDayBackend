package com.example.drmayday.features.authorization.controller;

import com.example.drmayday.features.authorization.dto.PatientProfileResponse;
import com.example.drmayday.features.authorization.dto.UpdateProfileRequest;
import com.example.drmayday.features.authorization.service.ProfileService;
import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý hồ sơ bệnh nhân cá nhân (Phân hệ Xác thực & Tài khoản).
 * Chứa API 7 (PROFILE_GET) và API 8 (PROFILE_PUT).
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * API 7: PROFILE_GET
     * Truy xuất thông tin hồ sơ chi tiết của bệnh nhân đang đăng nhập.
     * Dữ liệu kết hợp (JOIN) từ bảng gốc users và bảng chi tiết patients.
     *
     * @param token Bearer Token xác thực truyền qua Header Authorization
     * @return BaseResponse chứa PatientProfileResponse
     */
    @GetMapping
    public ResponseEntity<BaseResponse<PatientProfileResponse>> getProfile(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PatientProfileResponse response = profileService.getProfile(currentUserId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * API 8: PROFILE_PUT
     * Bệnh nhân chủ động cập nhật các thông tin nhân khẩu học (Năm sinh, Giới tính, Nghề nghiệp, Dân tộc),
     * địa chỉ liên hệ và thông tin Bảo hiểm y tế.
     *
     * @param token Bearer Token xác thực truyền qua Header Authorization
     * @param request Thông tin cập nhật hồ sơ
     * @return BaseResponse chứa PatientProfileResponse mới nhất
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
