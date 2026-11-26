package com.example.drmayday.features.admin.controller;

import com.example.drmayday.features.admin.dto.AssignUserRequest;
import com.example.drmayday.features.admin.dto.CreatePatientRequest;
import com.example.drmayday.features.admin.dto.CreatePersonRequest;
import com.example.drmayday.features.admin.dto.CreateStaffRequest;
import com.example.drmayday.features.admin.dto.CreateUserRequest;
import com.example.drmayday.features.admin.dto.CreatedUserResponse;
import com.example.drmayday.features.admin.dto.EditUserRequest;
import com.example.drmayday.features.admin.dto.StaffAssignResponse;
import com.example.drmayday.features.admin.dto.UserDetailsResponse;
import com.example.drmayday.features.admin.dto.UserResponse;
import com.example.drmayday.features.admin.service.AdminUserService;
import com.example.drmayday.features.profile.dto.EmployeeProfileResponse;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.PersonProfileResponse;
import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
                .andExpect(jsonPath("$.message").value("Thành công"))
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
                .andExpect(jsonPath("$.message").value("Không tìm thấy tài khoản người dùng"));
    }

    @Test
    @DisplayName("ADMIN_CREATE_USER: POST /api/v1/admin/users")
    void createUser_Success() throws Exception {
        CreateUserRequest req = new CreateUserRequest(17L, 2L, "doctor.17", "Password123@", true);
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-10-11T10:00:00Z");
        CreatedUserResponse created = new CreatedUserResponse(3L, 17L, 2L, "doctor.17", true, timestamp, timestamp);

        when(adminUserService.createUser(any(CreateUserRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.message").value("Thông báo tạo tài khoản thành công"))
                .andExpect(jsonPath("$.data.user_id").value(3))
                .andExpect(jsonPath("$.data.person_id").value(17))
                .andExpect(jsonPath("$.data.role_id").value(2))
                .andExpect(jsonPath("$.data.username").value("doctor.17"))
                .andExpect(jsonPath("$.data.is_active").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.password_hash").doesNotExist());
    }

    @Test
    @DisplayName("ADMIN_CREATE_USER: existing person already has an account — returns 409")
    void createUser_personAlreadyHasAccount_returnsConflict() throws Exception {
        CreateUserRequest req = new CreateUserRequest(17L, 2L, "doctor.17", "Password123@", null);

        when(adminUserService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new AppException(ErrorCode.PERSON_ALREADY_HAS_USER_ACCOUNT));

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("3011"))
                .andExpect(jsonPath("$.message").value("Người đã có tài khoản"));
    }

    @Test
    @DisplayName("ADMIN_CREATE_USER: missing person_id — returns validation error")
    void createUser_MissingRequiredParameter() throws Exception {
        CreateUserRequest req = new CreateUserRequest(null, 2L, "doctor.17", "Password123@", null);

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2001"))
                .andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc"));
    }

    @Test
    @DisplayName("ADMIN_CREATE_USER: person does not exist — returns 404")
    void createUser_personNotFound_returnsNotFound() throws Exception {
        CreateUserRequest req = new CreateUserRequest(999L, 2L, "doctor.17", "Password123@", null);
        when(adminUserService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new AppException(ErrorCode.PERSON_NOT_FOUND));

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("3014"));
    }

    @Test
    @DisplayName("ADMIN_CREATE_PERSON: valid request — returns created person")
    void createPerson_success() throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-10-11T10:00:00Z");
        when(adminUserService.createPerson(any(CreatePersonRequest.class))).thenReturn(
                new PersonProfileResponse(17L, "Nguyễn Văn A", LocalDate.of(1990, 1, 2), "Nam",
                        "0911111111", "Hà Nội", "Phường A", "Số 1", now, now));

        mockMvc.perform(post("/api/v1/admin/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name":"Nguyễn Văn A",
                                  "phone_number":"0911111111",
                                  "date_of_birth":"1990-01-02",
                                  "gender":"Nam",
                                  "province_city":"Hà Nội",
                                  "ward_commune":"Phường A",
                                  "address_line":"Số 1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.message").value("Thông báo tạo Người thành công"))
                .andExpect(jsonPath("$.data.person_id").value(17))
                .andExpect(jsonPath("$.data.phone_number").value("0911111111"));
    }

    @Test
    @DisplayName("ADMIN_CREATE_PERSON: missing full_name — returns validation error")
    void createPerson_missingFullName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone_number":"0911111111"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2001"));
    }

    @Test
    @DisplayName("ADMIN_CREATE_PATIENT: valid request — returns created patient profile")
    void createPatient_success() throws Exception {
        when(adminUserService.createPatient(any(CreatePatientRequest.class))).thenReturn(
                new PatientProfileResponse(17L, "HIS-17", "Nong dan", "Kinh", "BHYT-17",
                        LocalDate.of(2030, 1, 1), "Contact, address", "0987654321"));

        mockMvc.perform(post("/api/v1/admin/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "person_id":17,
                                  "his_code":"HIS-17",
                                  "occupation":"Nong dan",
                                  "ethnicity":"Kinh",
                                  "health_insurance_number":"BHYT-17",
                                  "health_insurance_valid_until":"2030-01-01",
                                  "emergency_contact_name_address":"Contact, address",
                                  "emergency_contact_phone":"0987654321"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.message").value("Thông báo tạo hồ sơ Bệnh nhân thành công"))
                .andExpect(jsonPath("$.data.person_id").value(17))
                .andExpect(jsonPath("$.data.his_code").value("HIS-17"));
    }

    @Test
    @DisplayName("ADMIN_CREATE_PATIENT: invalid occupation — returns validation error")
    void createPatient_invalidOccupation_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"person_id":17,"occupation":"Unknown"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2003"));
    }

    @Test
    @DisplayName("ADMIN_CREATE_STAFF: valid request — returns created staff profile")
    void createStaff_success() throws Exception {
        when(adminUserService.createStaff(any(CreateStaffRequest.class)))
                .thenReturn(new EmployeeProfileResponse(17L, 4L));

        mockMvc.perform(post("/api/v1/admin/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateStaffRequest(17L, 4L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.message").value("Thông báo tạo hồ sơ Nhân viên thành công"))
                .andExpect(jsonPath("$.data.person_id").value(17))
                .andExpect(jsonPath("$.data.branch_id").value(4));
    }

    @Test
    @DisplayName("ADMIN_CREATE_STAFF: branch_id missing — returns validation error")
    void createStaff_missingBranchId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"person_id\":17}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2001"));
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
                .andExpect(jsonPath("$.message").value("Không tìm thấy tài khoản người dùng"));
    }

    @Test
    @DisplayName("API 13: DELETE /api/v1/admin/users/{id} - Xóa tài khoản thành công (200 OK)")
    void deleteUser_Success() throws Exception {
        doNothing().when(adminUserService).deleteUser("202");

        mockMvc.perform(delete("/api/v1/admin/users/202")
                        .header("Authorization", "Bearer mock-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("Thành công"));

        verify(adminUserService).deleteUser("202");
    }

    @Test
    @DisplayName("API 13: DELETE /api/v1/admin/users/{id} - Tài khoản không tồn tại trả về 404 (3007)")
    void deleteUser_NotFound() throws Exception {
        doThrow(new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND))
                .when(adminUserService).deleteUser("999");

        mockMvc.perform(delete("/api/v1/admin/users/999")
                        .header("Authorization", "Bearer mock-admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("3007"));
    }

    @Test
    @DisplayName("API 14: POST /api/v1/admin/users/assign - Phân công nhân viên thành công (200 OK)")
    void assignUser_Success() throws Exception {
        StaffAssignResponse response = StaffAssignResponse.builder()
                .staffId("101")
                .userId("202")
                .branchId("7")
                .branchName("Chi nhánh Cầu Giấy")
                .build();
        when(adminUserService.assignUser(any(AssignUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users/assign")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\": 202, \"branch_id\": 7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.staff_id").value("101"))
                .andExpect(jsonPath("$.data.user_id").value("202"))
                .andExpect(jsonPath("$.data.branch_id").value("7"))
                .andExpect(jsonPath("$.data.branch_name").value("Chi nhánh Cầu Giấy"));
    }

    @Test
    @DisplayName("API 14: POST /api/v1/admin/users/assign - Thiếu branch_id trả về 400 (2001)")
    void assignUser_MissingBranchId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/assign")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\": 202}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2001"));
    }

    @Test
    @DisplayName("API 14: POST /api/v1/admin/users/assign - Chi nhánh không hợp lệ trả về 400 (2003)")
    void assignUser_InvalidBranch() throws Exception {
        when(adminUserService.assignUser(any(AssignUserRequest.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Chi nhánh không tồn tại"));

        mockMvc.perform(post("/api/v1/admin/users/assign")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\": 202, \"branch_id\": 999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2003"));
    }
}
