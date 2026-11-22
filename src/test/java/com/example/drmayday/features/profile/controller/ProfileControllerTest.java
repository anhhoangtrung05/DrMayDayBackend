package com.example.drmayday.features.profile.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.UpdateProfileRequest;
import com.example.drmayday.features.profile.service.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = ProfileControllerTest.TestWebConfiguration.class)
class ProfileControllerTest {

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @Import({ProfileController.class, GlobalExceptionHandler.class})
    static class TestWebConfiguration {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    private static final String BASE_URL = "/api/v1/profile";
    private static final Long USER_ID = 42L;
    private static final String PHONE = "0912345678";
    private static final String BEARER_TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @Nested
    @DisplayName("GET /api/v1/profile")
    class GetProfileTests {

        @Test
        @DisplayName("[Happy Path] Authenticated request — returns profile JSON")
        void getProfile_authenticatedUser_returnsProfile() throws Exception {
            PatientProfileResponse response = profileResponse();
            when(profileService.getProfile(USER_ID)).thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                            .header("Authorization", BEARER_TOKEN)
                            .with(user(testUser())))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.message").value("Thành công"))
                    .andExpect(jsonPath("$.data.user_id").value(USER_ID))
                    .andExpect(jsonPath("$.data.full_name").value("Test Patient"))
                    .andExpect(jsonPath("$.data.phone").value(PHONE))
                    .andExpect(jsonPath("$.data.gender").value("female"))
                    .andExpect(jsonPath("$.data.year_of_birth").value(1990))
                    .andExpect(jsonPath("$.data.address").value("123 Test Street"))
                    .andExpect(jsonPath("$.data.ethnic").value("Kinh"))
                    .andExpect(jsonPath("$.data.occupation").value("Doctor"))
                    .andExpect(jsonPath("$.data.bhyt_code").value("BH123456789"));

            verify(profileService).getProfile(USER_ID);
        }

        @Test
        @DisplayName("[Alternate Path] Authorization header omitted — authenticated principal still loads profile")
        void getProfile_authorizationHeaderOmitted_returnsProfile() throws Exception {
            when(profileService.getProfile(USER_ID)).thenReturn(profileResponse());

            mockMvc.perform(get(BASE_URL).with(user(testUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.user_id").value(USER_ID));

            verify(profileService).getProfile(USER_ID);
        }

        @Test
        @DisplayName("[Unhappy Path] Missing authenticated principal — returns unauthorized response")
        void getProfile_missingAuthentication_returnsUnauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("3003"))
                    .andExpect(jsonPath("$.message").value("Người dùng chưa được xác thực"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(profileService, never()).getProfile(any(Long.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Service reports missing account — returns not found response")
        void getProfile_accountNotFound_returnsNotFound() throws Exception {
            when(profileService.getProfile(USER_ID))
                    .thenThrow(new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

            mockMvc.perform(get(BASE_URL).with(user(testUser())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("3007"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy tài khoản người dùng"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void getProfile_serviceThrows_returnsStandardServerError() throws Exception {
            when(profileService.getProfile(USER_ID))
                    .thenThrow(new IllegalStateException("service unavailable"));

            mockMvc.perform(get(BASE_URL).with(user(testUser())))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/profile")
    class UpdateProfileTests {

        @Test
        @DisplayName("[Happy Path] Valid JSON request — returns updated profile")
        void updateProfile_validRequest_returnsUpdatedProfile() throws Exception {
            PatientProfileResponse response = profileResponse();
            response.setFullName("Updated Patient");
            response.setAddress("456 Updated Street");
            response.setYearOfBirth(1995);
            when(profileService.updateProfile(eq(USER_ID), any(UpdateProfileRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(put(BASE_URL)
                            .header("Authorization", BEARER_TOKEN)
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "full_name":"Updated Patient",
                                      "gender":"female",
                                      "year_of_birth":1995,
                                      "address":"456 Updated Street",
                                      "ethnic":"Kinh",
                                      "occupation":"Doctor",
                                      "bhyt_code":"BH987654321"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.message").value("Cập nhật thông tin thành công"))
                    .andExpect(jsonPath("$.data.user_id").value(USER_ID))
                    .andExpect(jsonPath("$.data.full_name").value("Updated Patient"))
                    .andExpect(jsonPath("$.data.year_of_birth").value(1995))
                    .andExpect(jsonPath("$.data.address").value("456 Updated Street"));

            ArgumentCaptor<UpdateProfileRequest> requestCaptor =
                    ArgumentCaptor.forClass(UpdateProfileRequest.class);
            verify(profileService).updateProfile(eq(USER_ID), requestCaptor.capture());
            assertEquals("Updated Patient", requestCaptor.getValue().getFullName());
            assertEquals("female", requestCaptor.getValue().getGender());
            assertEquals(Integer.valueOf(1995), requestCaptor.getValue().getYearOfBirth());
            assertEquals("456 Updated Street", requestCaptor.getValue().getAddress());
            assertEquals("Kinh", requestCaptor.getValue().getEthnic());
            assertEquals("Doctor", requestCaptor.getValue().getOccupation());
            assertEquals("BH987654321", requestCaptor.getValue().getBhytCode());
        }

        @Test
        @DisplayName("[Alternate Path] Partial JSON request — accepts omitted profile fields")
        void updateProfile_partialRequest_acceptsOmittedFields() throws Exception {
            when(profileService.updateProfile(eq(USER_ID), any(UpdateProfileRequest.class)))
                    .thenReturn(profileResponse());

            mockMvc.perform(put(BASE_URL)
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"address":"Updated Street"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"));

            ArgumentCaptor<UpdateProfileRequest> requestCaptor =
                    ArgumentCaptor.forClass(UpdateProfileRequest.class);
            verify(profileService).updateProfile(eq(USER_ID), requestCaptor.capture());
            assertEquals("Updated Street", requestCaptor.getValue().getAddress());
            assertNull(requestCaptor.getValue().getFullName());
            assertNull(requestCaptor.getValue().getYearOfBirth());
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid year-of-birth JSON type — returns bad request without service call")
        void updateProfile_invalidJsonType_returnsBadRequest() throws Exception {
            mockMvc.perform(put(BASE_URL)
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"year_of_birth":"not-a-number"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"))
                    .andExpect(jsonPath("$.message").value("Giá trị tham số không hợp lệ"));

            verify(profileService, never()).updateProfile(
                    any(Long.class), any(UpdateProfileRequest.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Service rejects profile data — returns bad request response")
        void updateProfile_serviceRejectsRequest_returnsBadRequest() throws Exception {
            when(profileService.updateProfile(eq(USER_ID), any(UpdateProfileRequest.class)))
                    .thenThrow(new AppException(
                            ErrorCode.INVALID_PARAMETER_VALUE, "Năm sinh không hợp lệ"));

            mockMvc.perform(put(BASE_URL)
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"year_of_birth":1800}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"))
                    .andExpect(jsonPath("$.message").value("Năm sinh không hợp lệ"));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void updateProfile_serviceThrows_returnsStandardServerError() throws Exception {
            doThrow(new IllegalStateException("service unavailable"))
                    .when(profileService).updateProfile(eq(USER_ID), any(UpdateProfileRequest.class));

            mockMvc.perform(put(BASE_URL)
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"address":"Updated Street"}
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    private static UserPrincipal testUser() {
        return UserPrincipal.create(USER_ID, PHONE, "patient");
    }

    private static PatientProfileResponse profileResponse() {
        return PatientProfileResponse.builder()
                .userId(USER_ID)
                .fullName("Test Patient")
                .phone(PHONE)
                .gender("female")
                .yearOfBirth(1990)
                .address("123 Test Street")
                .ethnic("Kinh")
                .occupation("Doctor")
                .bhytCode("BH123456789")
                .build();
    }
}
