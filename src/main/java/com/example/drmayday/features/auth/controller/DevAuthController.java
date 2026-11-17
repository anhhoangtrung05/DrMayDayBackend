package com.example.drmayday.features.auth.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.security.JwtTokenProvider;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Local development helper endpoint.
 * Allows developers to quickly generate JWTs for roles (admin, patient, doctor)
 * to test PROFILE_GET, PROFILE_PUT, ADMIN_GET_LIST_USERS, ADMIN_GET_USER_DETAILS,
 * ADMIN_CREATE_USER, and ADMIN_EDIT_USER with Postman, cURL, or an HTTP client without using the login API.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class DevAuthController {

    private final JwtTokenProvider jwtTokenProvider;

    public DevAuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Quickly generates a JWT for API testing. Supports query parameters and a JSON request body.
     *
     * @param userId User ID (defaults to 1 for admin and 2 for patient)
     * @param phone Phone number (defaults to 0900000001 for admin and 0912345678 for patient)
     * @param role Role: "admin", "patient", "benh_nhan", or "bac_si" (defaults to "admin")
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
