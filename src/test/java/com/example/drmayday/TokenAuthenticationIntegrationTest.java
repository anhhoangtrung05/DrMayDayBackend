package com.example.drmayday;

import com.example.drmayday.features.authorization.dto.CreateUserRequest;
import com.example.drmayday.features.authorization.dto.EditUserRequest;
import com.example.drmayday.features.authorization.dto.UpdateProfileRequest;
import com.example.drmayday.repository.UserAccountRepository;
import com.example.drmayday.features.security.JwtTokenProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
    private UserAccountRepository taiKhoanRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Kiểm thử trọn vẹn xác thực Token cho APIs 7-12 qua Filter Chain")
    void testFullApisTokenAuthentication() throws Exception {
        // Sinh tokens
        String patientToken = jwtTokenProvider.generateToken(2L, "0912345678", "patient");
        String adminToken = jwtTokenProvider.generateToken(1L, "0900000001", "admin");

        // -----------------------------------------------------------------
        // API 7: GET /api/v1/profile - Lấy thông tin hồ sơ bệnh nhân (200 OK)
        // -----------------------------------------------------------------
        mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.phone").value("0912345678"));

        // -----------------------------------------------------------------
        // API 8: PUT /api/v1/profile - Cập nhật hồ sơ bệnh nhân (200 OK)
        // -----------------------------------------------------------------
        UpdateProfileRequest updateProfileReq = UpdateProfileRequest.builder()
                .fullName("Bệnh Nhân Token Test")
                .yearOfBirth(1995)
                .gender("female")
                .ethnic("Kinh")
                .occupation("Nhân viên văn phòng")
                .address("456 Cầu Giấy, Hà Nội")
                .build();

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProfileReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.full_name").value("Bệnh Nhân Token Test"));

        // -----------------------------------------------------------------
        // API 9: GET /api/v1/admin/users - Admin lấy danh sách người dùng (200 OK)
        // -----------------------------------------------------------------
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "1")
                        .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.content").isArray());

        // -----------------------------------------------------------------
        // API 10: GET /api/v1/admin/users/{id} - Admin xem chi tiết người dùng (200 OK)
        // -----------------------------------------------------------------
        mockMvc.perform(get("/api/v1/admin/users/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.phone").value("0900000001"));

        // -----------------------------------------------------------------
        // API 11: POST /api/v1/admin/users - Admin tạo người dùng mới (201 Created)
        // -----------------------------------------------------------------
        String randomPhone = String.format("097%07d", (int) (System.currentTimeMillis() % 10000000L));
        CreateUserRequest createUserReq = CreateUserRequest.builder()
                .fullName("Người Dùng Test Token")
                .phone(randomPhone)
                .password("Password123@")
                .roleId("2") // Bác sĩ
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
        // API 12: PUT /api/v1/admin/users/{id} - Chỉnh sửa người dùng vừa tạo (200 OK)
        // -----------------------------------------------------------------
        EditUserRequest editUserReq = EditUserRequest.builder()
                .fullName("Người Dùng Đã Sửa Tên")
                .roleId("3") // Điều dưỡng
                .isActive("0") // Khóa tài khoản
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
        // KIỂM THỬ CÁC TÌNH HUỐNG LỖI XÁC THỰC THEO ĐẶC TẢ
        // -----------------------------------------------------------------

        // 1. Không truyền token -> Mã 3003 (User not authenticated)
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("3003"))
                .andExpect(jsonPath("$.message").value("User not authenticated"));

        // 2. Token không hợp lệ / sai chữ ký -> Mã 3001 (Token invalid)
        mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9.invalid.signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("3001"))
                .andExpect(jsonPath("$.message").value("Token invalid"));

        // 3. Token patient truy cập API Admin -> Mã 3102 (Admin role required - 403 Forbidden)
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3102"))
                .andExpect(jsonPath("$.message").value("Admin role required"));
    }

    @Test
    @DisplayName("API 11: Số điện thoại +84 được chuẩn hóa về 10 chữ số và không tạo trùng tài khoản")
    void testCreateUserNormalizesInternationalPhone() throws Exception {
        String adminToken = jwtTokenProvider.generateToken(1L, "0900000001", "admin");
        String subscriberDigits = String.format("96%07d", (int) (System.currentTimeMillis() % 10000000L));
        String localPhone = "0" + subscriberDigits;

        // Số dạng +84 được chấp nhận và lưu theo dạng 0xxxxxxxxx
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateUserRequest("+84" + subscriberDigits))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.phone").value(localPhone));

        assertEquals(true, taiKhoanRepository.existsByUsername(localPhone));

        // Cùng số đó ở dạng 0xxxxxxxxx bị từ chối vì đã tồn tại
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateUserRequest(localPhone))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("3006"));

        // Và ở chiều ngược lại, gửi lại dạng +84 cũng bị từ chối
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
