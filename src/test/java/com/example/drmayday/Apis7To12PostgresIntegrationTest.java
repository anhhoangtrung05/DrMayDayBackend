package com.example.drmayday;

import com.example.drmayday.features.admin.controller.AdminUserController;
import com.example.drmayday.features.profile.controller.ProfileController;
import com.example.drmayday.features.admin.dto.CreateUserRequest;
import com.example.drmayday.features.admin.dto.EditUserRequest;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.UpdateProfileRequest;
import com.example.drmayday.features.admin.dto.UserDetailsResponse;
import com.example.drmayday.features.admin.dto.UserResponse;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.repository.UserAccountRepository;
import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.common.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Apis7To12PostgresIntegrationTest {

    @Autowired
    private ProfileController profileController;

    @Autowired
    private AdminUserController adminUserController;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private com.example.drmayday.repository.PersonRepository personRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(Long userId, String phone, String role) {
        UserPrincipal principal = UserPrincipal.create(userId, phone, role);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("End-to-end integration test for profile and admin user endpoints against PostgreSQL")
    void testApis7To12AgainstPostgres() {
        // 1. Verify that the sample accounts were seeded.
        UserAccount patientAcc = userAccountRepository.findByUsername("0912345678")
                .orElseThrow(() -> new AssertionError("Patient account 0912345678 was not seeded"));
        UserAccount adminAcc = userAccountRepository.findByUsername("0900000001")
                .orElseThrow(() -> new AssertionError("Admin account 0900000001 was not seeded"));

        // Reset the patient data so this test can be rerun safely (idempotently).
        patientAcc.getPerson().setFullName("Nguyễn Văn A");
        personRepository.save(patientAcc.getPerson());

        // ==========================================
        // PROFILE_GET: PROFILE_GET
        // ==========================================
        authenticateUser(patientAcc.getAccountId(), "0912345678", "benh_nhan");
        ResponseEntity<BaseResponse<PatientProfileResponse>> getProfileRes = profileController.getProfile(null);
        assertEquals(HttpStatus.OK, getProfileRes.getStatusCode());
        assertNotNull(getProfileRes.getBody());
        assertEquals("1000", getProfileRes.getBody().getCode());
        PatientProfileResponse profile = getProfileRes.getBody().getData();
        assertNotNull(profile);
        assertEquals("Nguyễn Văn A", profile.getFullName());
        assertEquals("0912345678", profile.getPhone());
        assertEquals("Kinh", profile.getEthnic());

        // ==========================================
        // PROFILE_PUT: PROFILE_PUT
        // ==========================================
        UpdateProfileRequest updateReq = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn A Cập Nhật")
                .yearOfBirth(1992)
                .gender("male")
                .ethnic("Kinh")
                .occupation("Cong nhan")
                .bhytCode("DN4010999999999")
                .address("Số 10 Phố Huế, Hoàn Kiếm, Hà Nội")
                .build();
        ResponseEntity<BaseResponse<PatientProfileResponse>> updateProfileRes =
                profileController.updateProfile(null, updateReq);
        assertEquals(HttpStatus.OK, updateProfileRes.getStatusCode());
        assertEquals("1000", updateProfileRes.getBody().getCode());
        PatientProfileResponse updatedProfile = updateProfileRes.getBody().getData();
        assertEquals("Nguyễn Văn A Cập Nhật", updatedProfile.getFullName());
        assertEquals(1992, updatedProfile.getYearOfBirth());
        assertEquals("Cong nhan", updatedProfile.getOccupation());
        assertEquals("DN4010999999999", updatedProfile.getBhytCode());

        // ==========================================
        // ADMIN_GET_LIST_USERS
        // ==========================================
        authenticateUser(adminAcc.getAccountId(), "0900000001", "admin");
        ResponseEntity<BaseResponse<Page<UserResponse>>> listUsersRes =
                adminUserController.getListUsers(1, 10);
        assertEquals(HttpStatus.OK, listUsersRes.getStatusCode());
        assertEquals("1000", listUsersRes.getBody().getCode());
        Page<UserResponse> pageData = listUsersRes.getBody().getData();
        assertNotNull(pageData);
        assertTrue(pageData.getTotalElements() >= 2);

        // ==========================================
        // ADMIN_GET_USER_DETAILS
        // ==========================================
        ResponseEntity<BaseResponse<UserDetailsResponse>> userDetailsRes =
                adminUserController.getUserDetails(String.valueOf(adminAcc.getAccountId()));
        assertEquals(HttpStatus.OK, userDetailsRes.getStatusCode());
        assertEquals("1000", userDetailsRes.getBody().getCode());
        UserDetailsResponse adminDetails = userDetailsRes.getBody().getData();
        assertEquals("0900000001", adminDetails.getPhone());
        assertEquals("1", adminDetails.getRoleId());
        assertEquals("1", adminDetails.getIsActive());

        // ==========================================
        // ADMIN_CREATE_USER
        // ==========================================
        String testPhone = "0988776655";
        if (userAccountRepository.existsByUsername(testPhone)) {
            testPhone = String.format("098%07d", (int) (System.currentTimeMillis() % 10000000L));
        }
        CreateUserRequest createReq = CreateUserRequest.builder()
                .fullName("Bác Sĩ Test Postgres")
                .phone(testPhone)
                .password("Doctor@123")
                .roleId("2") // Doctor
                .isActive("1")
                .build();
        ResponseEntity<BaseResponse<UserResponse>> createRes =
                adminUserController.createUser(createReq);
        assertEquals(HttpStatus.CREATED, createRes.getStatusCode());
        assertEquals("1001", createRes.getBody().getCode());
        UserResponse createdUser = createRes.getBody().getData();
        assertNotNull(createdUser);
        assertNotNull(createdUser.getUserId());
        assertEquals("Bác Sĩ Test Postgres", createdUser.getFullName());
        assertEquals(testPhone, createdUser.getPhone());
        assertEquals("2", createdUser.getRoleId());
        assertEquals("1", createdUser.getIsActive());

        // ==========================================
        // ADMIN_EDIT_USER
        // ==========================================
        EditUserRequest editReq = EditUserRequest.builder()
                .fullName("Bác Sĩ Test Postgres (Đã Sửa)")
                .roleId("3") // Change to nurse
                .isActive("0") // Disable the account
                .build();
        ResponseEntity<BaseResponse<UserResponse>> editRes =
                adminUserController.editUser(createdUser.getUserId(), editReq);
        assertEquals(HttpStatus.OK, editRes.getStatusCode());
        assertEquals("1000", editRes.getBody().getCode());
        UserResponse editedUser = editRes.getBody().getData();
        assertEquals("Bác Sĩ Test Postgres (Đã Sửa)", editedUser.getFullName());
        assertEquals("3", editedUser.getRoleId());
        assertEquals("0", editedUser.getIsActive());
    }
}
