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
     * Sinh nhanh JWT Token để test API. Hỗ trợ cả Query Parameters lẫn JSON Request Body.
     *
     * @param userId ID người dùng (mặc định 1 cho admin, 2 cho patient)
     * @param phone Số điện thoại (mặc định 0900000001 cho admin, 0912345678 cho patient)
     * @param role Vai trò: "admin", "patient", "benh_nhan", "bac_si" (mặc định "admin")
     * @param body Optional JSON body
     * @return Token JWT Bearer
     */
    @RequestMapping(value = "/dev-token", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<BaseResponse<Map<String, Object>>> generateDevToken(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String role,
            @RequestBody(required = false) Map<String, Object> body) {

        Long resolvedUserId = userId;
        String resolvedPhone = phone;
        String resolvedRole = role;

        if (body != null) {
            if (resolvedUserId == null && body.containsKey("userId")) {
                Object u = body.get("userId");
                if (u instanceof Number num) resolvedUserId = num.longValue();
                else if (u instanceof String str && !str.isBlank()) resolvedUserId = Long.parseLong(str.trim());
            }
            if (resolvedUserId == null && body.containsKey("user_id")) {
                Object u = body.get("user_id");
                if (u instanceof Number num) resolvedUserId = num.longValue();
                else if (u instanceof String str && !str.isBlank()) resolvedUserId = Long.parseLong(str.trim());
            }
            if ((resolvedPhone == null || resolvedPhone.isBlank()) && body.containsKey("phone")) {
                resolvedPhone = String.valueOf(body.get("phone"));
            }
            if ((resolvedRole == null || resolvedRole.isBlank()) && body.containsKey("role")) {
                resolvedRole = String.valueOf(body.get("role"));
            }
        }

        if (resolvedRole == null || resolvedRole.isBlank()) {
            resolvedRole = "admin";
        }
        if (resolvedUserId == null) {
            resolvedUserId = "patient".equalsIgnoreCase(resolvedRole) || "benh_nhan".equalsIgnoreCase(resolvedRole) ? 2L : 1L;
        }
        if (resolvedPhone == null || resolvedPhone.isBlank()) {
            resolvedPhone = "patient".equalsIgnoreCase(resolvedRole) || "benh_nhan".equalsIgnoreCase(resolvedRole) ? "0912345678" : "0900000001";
        }

        String token = jwtTokenProvider.generateToken(resolvedUserId, resolvedPhone, resolvedRole);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("token_type", "Bearer");
        data.put("user_id", resolvedUserId);
        data.put("phone", resolvedPhone);
        data.put("role", resolvedRole);

        return ResponseEntity.ok(BaseResponse.success("Sinh Token thử nghiệm thành công", data));
    }
}
