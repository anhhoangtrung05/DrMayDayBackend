package com.example.drmayday.features.auth.controller;

import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.auth.dto.ChangePasswordRequest;
import com.example.drmayday.features.auth.dto.ForgotPasswordRequest;
import com.example.drmayday.features.auth.dto.LoginRequest;
import com.example.drmayday.features.auth.dto.LoginResponse;
import com.example.drmayday.features.auth.dto.ResetPasswordRequest;
import com.example.drmayday.features.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = AuthControllerTest.TestWebConfiguration.class)
class AuthControllerTest {

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @Import({AuthController.class, GlobalExceptionHandler.class})
    static class TestWebConfiguration {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    private static final String BASE_URL = "/api/v1/auth";
    private static final String PHONE = "0912345678";
    private static final String BEARER_TOKEN = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("[Happy Path] Valid JSON request — returns token response")
        void login_validRequest_returnsTokenResponse() throws Exception {
            when(authService.login(new LoginRequest(PHONE, "Password123!")))
                    .thenReturn(new LoginResponse(
                            "signed-token",
                            new LoginResponse.UserInfo(42L, "Test User", PHONE, 7L)
                    ));

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"0912345678","password":"Password123!"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.message").value("Thành công"))
                    .andExpect(jsonPath("$.data.token").value("signed-token"))
                    .andExpect(jsonPath("$.data.user_info.user_id").value(42))
                    .andExpect(jsonPath("$.data.user_info.full_name").value("Test User"))
                    .andExpect(jsonPath("$.data.user_info.phone").value(PHONE))
                    .andExpect(jsonPath("$.data.user_info.role_id").value(7));

            verify(authService).login(new LoginRequest(PHONE, "Password123!"));
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid phone format — returns validation error")
        void login_invalidPhone_returnsValidationError() throws Exception {
            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"12345","password":"Password123!"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2004"))
                    .andExpect(jsonPath("$.message").value("Định dạng số điện thoại không hợp lệ"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(authService, never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Missing password — returns required parameter error")
        void login_missingPassword_returnsRequiredParameterError() throws Exception {
            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"0912345678"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"))
                    .andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc"));

            verify(authService, never()).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void login_serviceThrows_returnsStandardServerError() throws Exception {
            when(authService.login(new LoginRequest(PHONE, "Password123!")))
                    .thenThrow(new IllegalStateException("service unavailable"));

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"0912345678","password":"Password123!"}
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("[Happy Path] Authorization header provided — forwards header and returns success")
        void logout_authorizationHeaderProvided_forwardsHeaderAndReturnsSuccess() throws Exception {
            mockMvc.perform(post(BASE_URL + "/logout")
                            .header("Authorization", BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.message").value("Đăng xuất thành công"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(authService).logout(BEARER_TOKEN);
        }

        @Test
        @DisplayName("[Alternate Path] Authorization header omitted — passes null and returns success")
        void logout_authorizationHeaderOmitted_passesNullAndReturnsSuccess() throws Exception {
            mockMvc.perform(post(BASE_URL + "/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

            verify(authService).logout(null);
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void logout_serviceThrows_returnsStandardServerError() throws Exception {
            doThrow(new IllegalStateException("service unavailable"))
                    .when(authService).logout(BEARER_TOKEN);

            mockMvc.perform(post(BASE_URL + "/logout")
                            .header("Authorization", BEARER_TOKEN))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("[Happy Path] Valid phone — returns recovery confirmation")
        void forgotPassword_validPhone_returnsRecoveryConfirmation() throws Exception {
            mockMvc.perform(post(BASE_URL + "/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"0912345678"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.message").value("Đã gửi hướng dẫn khôi phục mật khẩu"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(authService).forgotPassword(new ForgotPasswordRequest(PHONE));
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid phone — returns validation error without service call")
        void forgotPassword_invalidPhone_returnsValidationErrorWithoutServiceCall() throws Exception {
            mockMvc.perform(post(BASE_URL + "/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"not-a-phone"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2004"))
                    .andExpect(jsonPath("$.message").value("Định dạng số điện thoại không hợp lệ"));

            verify(authService, never()).forgotPassword(any(ForgotPasswordRequest.class));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void forgotPassword_serviceThrows_returnsStandardServerError() throws Exception {
            doThrow(new IllegalStateException("service unavailable"))
                    .when(authService).forgotPassword(new ForgotPasswordRequest(PHONE));

            mockMvc.perform(post(BASE_URL + "/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"0912345678"}
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("[Happy Path] Valid reset payload — returns reset confirmation")
        void resetPassword_validPayload_returnsResetConfirmation() throws Exception {
            mockMvc.perform(post(BASE_URL + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "phone":"0912345678",
                                      "reset_token":"123456",
                                      "new_password":"NewPassword123!",
                                      "confirm_password":"NewPassword123!"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công"))
                    .andExpect(jsonPath("$.data").isMap())
                    .andExpect(jsonPath("$.data").isEmpty());

            verify(authService).resetPassword(new ResetPasswordRequest(
                    PHONE, "123456", "NewPassword123!", "NewPassword123!"));
        }

        @Test
        @DisplayName("[Unhappy Path] Missing reset token — returns required parameter error")
        void resetPassword_missingToken_returnsRequiredParameterError() throws Exception {
            mockMvc.perform(post(BASE_URL + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "phone":"0912345678",
                                      "new_password":"NewPassword123!",
                                      "confirm_password":"NewPassword123!"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"))
                    .andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc"));

            verify(authService, never()).resetPassword(any(ResetPasswordRequest.class));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void resetPassword_serviceThrows_returnsStandardServerError() throws Exception {
            doThrow(new IllegalStateException("service unavailable"))
                    .when(authService).resetPassword(new ResetPasswordRequest(
                            PHONE, "123456", "NewPassword123!", "NewPassword123!"));

            mockMvc.perform(post(BASE_URL + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "phone":"0912345678",
                                      "reset_token":"123456",
                                      "new_password":"NewPassword123!",
                                      "confirm_password":"NewPassword123!"
                                    }
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/auth/change-password")
    class ChangePasswordTests {

        @Test
        @DisplayName("[Happy Path] Authorization header and payload — forwards header and returns success")
        void changePassword_authorizationHeaderAndPayload_forwardsHeaderAndReturnsSuccess() throws Exception {
            mockMvc.perform(put(BASE_URL + "/change-password")
                            .header("Authorization", BEARER_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "current_password":"OldPassword123!",
                                      "new_password":"NewPassword123!",
                                      "confirm_password":"NewPassword123!"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(authService).changePassword(
                    BEARER_TOKEN,
                    new ChangePasswordRequest(
                            "OldPassword123!", "NewPassword123!", "NewPassword123!"));
        }

        @Test
        @DisplayName("[Unhappy Path] Blank required field — returns validation error without service call")
        void changePassword_blankRequiredField_returnsValidationErrorWithoutServiceCall() throws Exception {
            mockMvc.perform(put(BASE_URL + "/change-password")
                            .header("Authorization", BEARER_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "current_password":"",
                                      "new_password":"NewPassword123!",
                                      "confirm_password":"NewPassword123!"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"))
                    .andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc"));

            verify(authService, never()).changePassword(
                    anyString(), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void changePassword_serviceThrows_returnsStandardServerError() throws Exception {
            doThrow(new IllegalStateException("service unavailable"))
                    .when(authService).changePassword(
                            BEARER_TOKEN,
                            new ChangePasswordRequest(
                                    "OldPassword123!", "NewPassword123!", "NewPassword123!"));

            mockMvc.perform(put(BASE_URL + "/change-password")
                            .header("Authorization", BEARER_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "current_password":"OldPassword123!",
                                      "new_password":"NewPassword123!",
                                      "confirm_password":"NewPassword123!"
                                    }
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"));
        }
    }

}
