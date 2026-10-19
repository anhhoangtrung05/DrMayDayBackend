package com.drmayday.authrbac.controller;

import com.drmayday.authrbac.dto.CreateUserRequest;
import com.drmayday.authrbac.dto.EditUserRequest;
import com.drmayday.authrbac.dto.UserDetailsResponse;
import com.drmayday.authrbac.dto.UserResponse;
import com.drmayday.authrbac.service.AdminUserService;
import com.drmayday.common.exception.AppException;
import com.drmayday.common.exception.ErrorCode;
import com.drmayday.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API 9: GET /api/v1/admin/users - Admin lấy danh sách thành công")
    void getListUsers_Success() throws Exception {
        UserResponse user = UserResponse.builder()
                .userId("1")
                .fullName("Nguyễn Văn A")
                .phone("0912345678")
                .roleId("2")
                .isActive("1")
                .createdAt("2026-10-10 10:00:00")
                .build();

        when(adminUserService.getListUsers(1, 20))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/users?page=1&limit=20")
                        .header("Authorization", "Bearer mock-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.content[0].user_id").value("1"))
                .andExpect(jsonPath("$.data.content[0].full_name").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.content[0].phone").value("0912345678"))
                .andExpect(jsonPath("$.data.content[0].role_id").value("2"))
                .andExpect(jsonPath("$.data.content[0].is_active").value("1"));
    }

    @Test
    @DisplayName("API 10: GET /api/v1/admin/users/{id} - Lấy chi tiết user thành công")
    void getUserDetails_Success() throws Exception {
        UserDetailsResponse details = UserDetailsResponse.builder()
                .userId("1")
                .fullName("Nguyễn Văn A")
                .phone("0912345678")
                .roleId("2")
                .isActive("1")
                .createdAt("2026-10-10 10:00:00")
                .updatedAt("2026-10-10 10:00:00")
                .build();

        when(adminUserService.getUserDetails("1")).thenReturn(details);

        mockMvc.perform(get("/api/v1/admin/users/1")
                        .header("Authorization", "Bearer mock-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.user_id").value("1"))
                .andExpect(jsonPath("$.data.full_name").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.phone").value("0912345678"))
                .andExpect(jsonPath("$.data.role_id").value("2"))
                .andExpect(jsonPath("$.data.is_active").value("1"));
    }

    @Test
    @DisplayName("API 10: GET /api/v1/admin/users/{id} - ID không tồn tại trả về 404")
    void getUserDetails_NotFound() throws Exception {
        when(adminUserService.getUserDetails("999"))
                .thenThrow(new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/users/999")
                        .header("Authorization", "Bearer mock-admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("3007"))
                .andExpect(jsonPath("$.message").value("User account not found"));
    }

    @Test
    @DisplayName("API 11: POST /api/v1/admin/users - Tạo người dùng thành công trả về 201 Created")
    void createUser_Success() throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .fullName("Trần Thị B")
                .phone("0987654321")
                .password("Password123@")
                .roleId("2")
                .isActive("1")
                .build();

        UserResponse created = UserResponse.builder()
                .userId("2")
                .fullName("Trần Thị B")
                .phone("0987654321")
                .roleId("2")
                .isActive("1")
                .createdAt("2026-10-12 10:00:00")
                .build();

        when(adminUserService.createUser(any(CreateUserRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.message").value("Created successfully"))
                .andExpect(jsonPath("$.data.user_id").value("2"))
                .andExpect(jsonPath("$.data.full_name").value("Trần Thị B"))
                .andExpect(jsonPath("$.data.phone").value("0987654321"));
    }

    @Test
    @DisplayName("API 11: POST /api/v1/admin/users - Số điện thoại đã tồn tại trả về 409 Conflict")
    void createUser_PhoneAlreadyExists() throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .fullName("Trần Thị B")
                .phone("0987654321")
                .password("Password123@")
                .roleId("2")
                .isActive("1")
                .build();

        when(adminUserService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new AppException(ErrorCode.USER_PHONE_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("3006"))
                .andExpect(jsonPath("$.message").value("User phone already exists"));
    }

    @Test
    @DisplayName("API 11: POST /api/v1/admin/users - Thiếu trường bắt buộc trả về 400 và mã 2001")
    void createUser_MissingRequiredParameter() throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .fullName("")
                .phone("0987654321")
                .password("Password123@")
                .roleId("2")
                .build();

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2001"))
                .andExpect(jsonPath("$.message").value("Missing required parameter"));
    }

    @Test
    @DisplayName("API 11: POST /api/v1/admin/users - Số điện thoại sai định dạng trả về 400 và mã 2004")
    void createUser_InvalidPhoneFormat() throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .fullName("Nguyễn Văn A")
                .phone("12345")
                .password("Password123@")
                .roleId("2")
                .build();

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2004"))
                .andExpect(jsonPath("$.message").value("Invalid phone number format"));
    }

    @Test
    @DisplayName("API 12: PUT /api/v1/admin/users/{id} - Chỉnh sửa thông tin thành công")
    void editUser_Success() throws Exception {
        EditUserRequest req = EditUserRequest.builder()
                .fullName("Nguyễn Văn A Cập Nhật")
                .roleId("1")
                .isActive("1")
                .build();

        UserResponse updated = UserResponse.builder()
                .userId("1")
                .fullName("Nguyễn Văn A Cập Nhật")
                .roleId("1")
                .isActive("1")
                .updatedAt("2026-10-12 11:00:00")
                .build();

        when(adminUserService.editUser(eq("1"), any(EditUserRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/users/1")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.user_id").value("1"))
                .andExpect(jsonPath("$.data.full_name").value("Nguyễn Văn A Cập Nhật"))
                .andExpect(jsonPath("$.data.role_id").value("1"))
                .andExpect(jsonPath("$.data.is_active").value("1"));
    }

    @Test
    @DisplayName("API 12: PUT /api/v1/admin/users/{id} - Người dùng không tồn tại trả về 404")
    void editUser_NotFound() throws Exception {
        EditUserRequest req = EditUserRequest.builder()
                .fullName("Nguyễn Văn A")
                .build();

        when(adminUserService.editUser(eq("999"), any(EditUserRequest.class)))
                .thenThrow(new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        mockMvc.perform(put("/api/v1/admin/users/999")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("3007"))
                .andExpect(jsonPath("$.message").value("User account not found"));
    }
}
