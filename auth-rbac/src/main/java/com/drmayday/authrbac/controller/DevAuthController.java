package com.drmayday.authrbac.controller;

import com.drmayday.authrbac.security.JwtTokenProvider;
import com.drmayday.common.dto.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint hỗ trợ phát triển local (Dev Local Helper):
 * Cho phép lập trình viên tạo nhanh JWT token cho các vai trò (admin, patient, doctor)
 * để kiểm thử các APIs 7-12 qua Postman / cURL / HTTP Client mà không cần đợi API Login.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class DevAuthController {

    private final JwtTokenProvider jwtTokenProvider;

    public DevAuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Sinh nhanh JWT Token để test API.
     *
     * @param userId ID người dùng (mặc định 1 cho admin, 2 cho patient)
     * @param phone Số điện thoại (mặc định 0900000001 cho admin, 0912345678 cho patient)
     * @param role Vai trò: "admin" hoặc "patient" (mặc định "admin")
     * @return Token JWT Bearer
     */
    @PostMapping("/dev-token")
    public ResponseEntity<BaseResponse<Map<String, Object>>> generateDevToken(
            @RequestParam(defaultValue = "1") Integer userId,
            @RequestParam(defaultValue = "0900000001") String phone,
            @RequestParam(defaultValue = "admin") String role) {

        String token = jwtTokenProvider.generateToken(userId, phone, role);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("token_type", "Bearer");
        data.put("user_id", userId);
        data.put("phone", phone);
        data.put("role", role);

        return ResponseEntity.ok(BaseResponse.success("Sinh Token thử nghiệm thành công", data));
    }
}
