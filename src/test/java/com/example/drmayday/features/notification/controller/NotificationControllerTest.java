package com.example.drmayday.features.notification.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.notification.dto.FcmTokenResponse;
import com.example.drmayday.features.notification.dto.NotificationResponse;
import com.example.drmayday.features.notification.dto.SetDevTokenRequest;
import com.example.drmayday.features.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("API 100: POST /api/v1/notifications/token")
    class SetDevTokenTests {

        @Test
        @DisplayName("[Happy Path] Registers FCM token and returns 201 (1001)")
        void setDevToken_success() throws Exception {
            OffsetDateTime now = OffsetDateTime.parse("2026-11-28T02:00:00Z");
            when(notificationService.registerDeviceToken(any(SetDevTokenRequest.class)))
                    .thenReturn(FcmTokenResponse.builder()
                            .id("1")
                            .userId("2")
                            .fcmToken("fcm-token-123")
                            .deviceType("android")
                            .createdAt(now)
                            .updatedAt(now)
                            .build());

            mockMvc.perform(post("/api/v1/notifications/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fcm_token\":\"fcm-token-123\",\"device_type\":\"android\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("1001"))
                    .andExpect(jsonPath("$.data.id").value("1"))
                    .andExpect(jsonPath("$.data.user_id").value("2"))
                    .andExpect(jsonPath("$.data.fcm_token").value("fcm-token-123"))
                    .andExpect(jsonPath("$.data.device_type").value("android"));
        }

        @Test
        @DisplayName("[Unhappy Path] Missing fcm_token returns 400 (9101)")
        void setDevToken_missingFcmToken_returns9101() throws Exception {
            when(notificationService.registerDeviceToken(any()))
                    .thenThrow(new AppException(ErrorCode.FCM_TOKEN_REQUIRED));

            mockMvc.perform(post("/api/v1/notifications/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fcm_token\":\"\",\"device_type\":\"android\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("9101"));
        }
    }

    @Nested
    @DisplayName("API 101: GET /api/v1/notifications/")
    class GetNotificationsTests {

        @Test
        @DisplayName("[Happy Path] Supports both /api/v1/notifications/ and /api/v1/notifications")
        void getNotifications_success() throws Exception {
            OffsetDateTime now = OffsetDateTime.parse("2026-11-28T02:00:00Z");
            when(notificationService.getMyNotifications("1", "20")).thenReturn(List.of(
                    NotificationResponse.builder()
                            .id("10")
                            .userId("2")
                            .title("Lịch tái khám")
                            .body("Nhắc lịch hẹn")
                            .type("appointment")
                            .avatar(null)
                            .referenceId("5")
                            .isRead("0")
                            .createdAt(now)
                            .updatedAt(now)
                            .build()
            ));

            mockMvc.perform(get("/api/v1/notifications/")
                            .param("page", "1")
                            .param("limit", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value("10"))
                    .andExpect(jsonPath("$.data[0].is_read").value("0"))
                    .andExpect(jsonPath("$.data[0].avatar").isEmpty());

            mockMvc.perform(get("/api/v1/notifications")
                            .param("page", "1")
                            .param("limit", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"));
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid page param returns 400 (2002)")
        void getNotifications_invalidPage_returns2002() throws Exception {
            when(notificationService.getMyNotifications("abc", null))
                    .thenThrow(new AppException(ErrorCode.INVALID_PARAMETER_TYPE));

            mockMvc.perform(get("/api/v1/notifications/")
                            .param("page", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2002"));
        }
    }

    @Nested
    @DisplayName("API 102: PUT /api/v1/notifications/{id}/read")
    class MarkAsReadTests {

        @Test
        @DisplayName("[Happy Path] Marks notification read and returns 200 (1000) with is_read='1'")
        void setReadNotification_success() throws Exception {
            when(notificationService.markAsRead("10")).thenReturn(NotificationResponse.builder()
                    .id("10")
                    .userId("2")
                    .title("Lịch tái khám")
                    .body("Nhắc lịch hẹn")
                    .type("appointment")
                    .isRead("1")
                    .build());

            mockMvc.perform(put("/api/v1/notifications/10/read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.id").value("10"))
                    .andExpect(jsonPath("$.data.is_read").value("1"));
        }

        @Test
        @DisplayName("[Unhappy Path] Missing or foreign notification returns 404 (9102)")
        void setReadNotification_notFound_returns9102() throws Exception {
            when(notificationService.markAsRead("999"))
                    .thenThrow(new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

            mockMvc.perform(put("/api/v1/notifications/999/read"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("9102"));
        }
    }

    @Nested
    @DisplayName("API 103: DELETE /api/v1/notifications/{id}")
    class DeleteNotificationTests {

        @Test
        @DisplayName("[Happy Path] Deletes notification and returns 200 (1000)")
        void deleteNotification_success() throws Exception {
            doNothing().when(notificationService).deleteNotification("10");

            mockMvc.perform(delete("/api/v1/notifications/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"));
        }

        @Test
        @DisplayName("[Unhappy Path] Missing or foreign notification returns 404 (9102)")
        void deleteNotification_notFound_returns9102() throws Exception {
            doThrow(new AppException(ErrorCode.NOTIFICATION_NOT_FOUND))
                    .when(notificationService).deleteNotification("999");

            mockMvc.perform(delete("/api/v1/notifications/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("9102"));
        }
    }
}
