package com.drmayday.authrbac.controller;

import com.drmayday.authrbac.dto.PatientProfileResponse;
import com.drmayday.authrbac.dto.UpdateProfileRequest;
import com.drmayday.authrbac.service.ProfileService;
import com.drmayday.common.exception.AppException;
import com.drmayday.common.exception.ErrorCode;
import com.drmayday.common.exception.GlobalExceptionHandler;
import com.drmayday.common.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(Integer userId, String phone, String role) {
        UserPrincipal principal = UserPrincipal.create(userId, phone, phone, role);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("API 7: GET /api/v1/profile - Lấy hồ sơ bệnh nhân thành công (200 OK)")
    void getProfile_Success() throws Exception {
        authenticateUser(1, "0912345678", "patient");

        PatientProfileResponse mockResponse = PatientProfileResponse.builder()
                .userId(1)
                .fullName("Nguyễn Văn A")
                .phone("0912345678")
                .gender("male")
                .yearOfBirth(1990)
                .address("123 Đường ABC, Hà Nội")
                .ethnic("Kinh")
                .occupation("Kỹ sư")
                .bhytCode("GD4010123456789")
                .build();

        when(profileService.getProfile(1)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.user_id").value(1))
                .andExpect(jsonPath("$.data.full_name").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.phone").value("0912345678"))
                .andExpect(jsonPath("$.data.gender").value("male"))
                .andExpect(jsonPath("$.data.year_of_birth").value(1990))
                .andExpect(jsonPath("$.data.address").value("123 Đường ABC, Hà Nội"))
                .andExpect(jsonPath("$.data.ethnic").value("Kinh"))
                .andExpect(jsonPath("$.data.occupation").value("Kỹ sư"))
                .andExpect(jsonPath("$.data.bhyt_code").value("GD4010123456789"));
    }

    @Test
    @DisplayName("API 7: GET /api/v1/profile - Chưa đăng nhập trả về 401 Unauthorized")
    void getProfile_Unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("3003"))
                .andExpect(jsonPath("$.message").value("User not authenticated"));
    }

    @Test
    @DisplayName("API 7: GET /api/v1/profile - Tài khoản không tồn tại trả về 404 Not Found")
    void getProfile_UserNotFound() throws Exception {
        authenticateUser(99, "0999999999", "patient");

        when(profileService.getProfile(99))
                .thenThrow(new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("3007"))
                .andExpect(jsonPath("$.message").value("User account not found"));
    }

    @Test
    @DisplayName("API 8: PUT /api/v1/profile - Cập nhật hồ sơ thành công (200 OK)")
    void updateProfile_Success() throws Exception {
        authenticateUser(1, "0912345678", "patient");

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn B")
                .yearOfBirth(1995)
                .address("456 Đường XYZ, Hà Nội")
                .gender("male")
                .ethnic("Kinh")
                .occupation("Lập trình viên")
                .bhytCode("HS4010123456789")
                .build();

        PatientProfileResponse mockResponse = PatientProfileResponse.builder()
                .userId(1)
                .fullName("Nguyễn Văn B")
                .phone("0912345678")
                .gender("male")
                .yearOfBirth(1995)
                .address("456 Đường XYZ, Hà Nội")
                .ethnic("Kinh")
                .occupation("Lập trình viên")
                .bhytCode("HS4010123456789")
                .build();

        when(profileService.updateProfile(eq(1), any(UpdateProfileRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin thành công"))
                .andExpect(jsonPath("$.data.user_id").value(1))
                .andExpect(jsonPath("$.data.full_name").value("Nguyễn Văn B"))
                .andExpect(jsonPath("$.data.year_of_birth").value(1995))
                .andExpect(jsonPath("$.data.address").value("456 Đường XYZ, Hà Nội"));
    }

    @Test
    @DisplayName("API 8: PUT /api/v1/profile - Giá trị tham số không hợp lệ trả về 400")
    void updateProfile_InvalidParam() throws Exception {
        authenticateUser(1, "0912345678", "patient");

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .yearOfBirth(1800)
                .build();

        when(profileService.updateProfile(eq(1), any(UpdateProfileRequest.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Năm sinh không hợp lệ"));

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2003"));
    }
}
