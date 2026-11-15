package com.example.drmayday;

import com.example.drmayday.features.admin.dto.CreateUserRequest;
import com.example.drmayday.features.admin.dto.EditUserRequest;
import com.example.drmayday.features.profile.dto.UpdateProfileRequest;
import com.example.drmayday.repository.UserAccountRepository;
import com.example.drmayday.features.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TokenAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("End-to-end token authentication test for profile and admin user endpoints through the filter chain")
    void testFullApisTokenAuthentication() throws Exception {
        // Generate tokens.
        String patientToken = jwtTokenProvider.generateToken(2L, "0912345678", "patient");
        String adminToken = jwtTokenProvider.generateToken(1L, "0900000001", "admin");

        // -----------------------------------------------------------------
        // PROFILE_GET: GET /api/v1/profile - Retrieve the patient's profile (200 OK).
        // -----------------------------------------------------------------
        mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("Thành công"))
                .andExpect(jsonPath("$.data.phone").value("0912345678"));

        // -----------------------------------------------------------------
        // PROFILE_PUT: PUT /api/v1/profile - Update the patient's profile (200 OK).
        // -----------------------------------------------------------------
        UpdateProfileRequest updateProfileReq = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn Bệnh Nhân E2E")
                .yearOfBirth(1993)
                .gender("Nam")
                .ethnic("Kinh")
                .occupation("Lập trình viên")
                .address("Tòa nhà FPT, Cầu Giấy, Hà Nội")
                .bhytCode("DN4010888888888")
                .build();

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProfileReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.full_name").value("Nguyễn Văn Bệnh Nhân E2E"))
                .andExpect(jsonPath("$.data.year_of_birth").value(1993));

        // -----------------------------------------------------------------
        // ADMIN_GET_LIST_USERS: GET /api/v1/admin/users - Admin retrieves the user list (200 OK).
        // -----------------------------------------------------------------
        mockMvc.perform(get("/api/v1/admin/users?page=1&limit=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.content").isArray());

        // -----------------------------------------------------------------
        // ADMIN_GET_USER_DETAILS: GET /api/v1/admin/users/1 - Retrieve admin account details (200 OK).
        // -----------------------------------------------------------------
        mockMvc.perform(get("/api/v1/admin/users/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.phone").value("0900000001"));

        // -----------------------------------------------------------------
        // ADMIN_CREATE_USER: POST /api/v1/admin/users - Admin creates a user (201 Created).
        // -----------------------------------------------------------------
        String randomPhone = String.format("097%07d", (int) (System.currentTimeMillis() % 10000000L));
        CreateUserRequest createUserReq = CreateUserRequest.builder()
                .fullName("Người Dùng Test Token")
                .phone(randomPhone)
                .password("Password123@")
                .roleId("2") // Doctor
                .isActive("1")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.phone").value(randomPhone))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String newUserId = createdJson.path("data").path("user_id").asText();
        assertNotNull(newUserId);

        // -----------------------------------------------------------------
        // ADMIN_EDIT_USER: PUT /api/v1/admin/users/{id} - Update the newly created user (200 OK).
        // -----------------------------------------------------------------
        EditUserRequest editUserReq = EditUserRequest.builder()
                .fullName("Người Dùng Đã Sửa Tên")
                .roleId("3") // Nurse
                .isActive("0") // Disable the account
                .build();

        mockMvc.perform(put("/api/v1/admin/users/" + newUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editUserReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.full_name").value("Người Dùng Đã Sửa Tên"))
                .andExpect(jsonPath("$.data.role_id").value("3"))
                .andExpect(jsonPath("$.data.is_active").value("0"));

        // -----------------------------------------------------------------
        // Test authentication failure scenarios from the specification.
        // -----------------------------------------------------------------

        // 1. No token supplied -> code 3003 (User not authenticated).
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("3003"))
                .andExpect(jsonPath("$.message").value("Người dùng chưa được xác thực"));

        // 2. Invalid token or signature -> code 3001 (Token invalid).
        mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9.invalid.signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("3001"))
                .andExpect(jsonPath("$.message").value("Token không hợp lệ"));

        // 3. Patient token accesses an admin API -> code 3102 (Admin role required - 403 Forbidden).
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3102"))
                .andExpect(jsonPath("$.message").value("Yêu cầu quyền quản trị viên"));
    }

    @Test
    @DisplayName("ADMIN_CREATE_USER: +84 phone numbers are normalized to 10 digits and duplicate accounts are rejected")
    void testCreateUserNormalizesInternationalPhone() throws Exception {
        String adminToken = jwtTokenProvider.generateToken(1L, "0900000001", "admin");
        String subscriberDigits = String.format("96%07d", (int) (System.currentTimeMillis() % 10000000L));
        String localPhone = "0" + subscriberDigits;

        // A +84 number is accepted and stored in 0xxxxxxxxx format.
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateUserRequest("+84" + subscriberDigits))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.phone").value(localPhone));

        assertEquals(true, userAccountRepository.existsByUsername(localPhone));

        // The same number in 0xxxxxxxxx format is rejected because it already exists.
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateUserRequest(localPhone))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("3006"));

        // The reverse also applies: resubmitting the number in +84 format is rejected.
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateUserRequest("+84" + subscriberDigits))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("3006"));
    }

    private CreateUserRequest buildCreateUserRequest(String phone) {
        return CreateUserRequest.builder()
                .fullName("Người Dùng Số Quốc Tế")
                .phone(phone)
                .password("Password123@")
                .roleId("2")
                .isActive("1")
                .build();
    }
}
